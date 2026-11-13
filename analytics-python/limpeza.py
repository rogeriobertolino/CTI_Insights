"""Normalização determinística; preserva linha e valores originais para auditoria."""
import re
import math
import unicodedata
from datetime import datetime, date

ALIASES = {"codigo": "codigo_cliente", "cliente_codigo": "codigo_cliente", "cod_cliente": "codigo_cliente", "nome": "cliente", "nome_cliente": "cliente", "razao_social": "cliente", "nivel_cliente": "nivel", "faturamento": "faturamento_mensal", "receita": "faturamento_mensal"}
CONTRACT_FIELDS = {"valor_servico", "status_contrato", "data_inicio"}
REQUIRED = ("codigo_cliente", "cliente", "consultor", "segmento", "nivel", "faturamento_mensal", "servico", "valor_servico", "status_contrato", "data_inicio")

def chave(value):
    value = unicodedata.normalize("NFD", str(value)).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "_", value).strip("_")

def dinheiro(value):
    from decimal import Decimal, InvalidOperation
    text = str(value).replace("R$", "").replace(" ", "").strip()
    if "," in text and "." in text:
        if text.rfind(",") > text.rfind("."): text = text.replace(".", "").replace(",", ".")
        else: text = text.replace(",", "")
    else: text = text.replace(",", ".")
    if not re.fullmatch(r"[+-]?\d+(?:\.\d{1,2})?", text): return None
    try:
        number = Decimal(text)
        return float(number) if number.is_finite() and abs(number) < Decimal("10000000000000") else None
    except InvalidOperation: return None

def data_iso(value):
    if isinstance(value, (datetime, date)): return value.date().isoformat() if isinstance(value, datetime) else value.isoformat()
    text = str(value).strip()
    for fmt in ("%Y-%m-%d", "%Y-%m-%d %H:%M:%S", "%d/%m/%Y"):
        try: return datetime.strptime(text, fmt).date().isoformat()
        except ValueError: pass
    return None

def limpar_dados(records):
    import pandas as pd
    frame = pd.DataFrame.from_records(records).fillna('')
    result = []
    for index, original in enumerate(frame.to_dict(orient='records')):
        row = {}
        for key, value in original.items():
            field = ALIASES.get(chave(key), chave(key))
            if field in row: raise ValueError("Colunas equivalentes duplicadas: " + field)
            row[field] = " ".join(str(value if value is not None else "").split())
        row["linha_origem"] = original.get("linha_origem", index + 2)
        if "segmento" in row:
            row["segmento"] = {"ind": "Indústria", "industria": "Indústria", "saude": "Saúde", "varejo": "Varejo", "comercio": "Comércio", "servicos": "Serviços"}.get(chave(row["segmento"]), row["segmento"])
        for field in ("nivel", "status_contrato"):
            if field in row: row[field] = row[field].upper()
        for field in ("faturamento_mensal", "valor_servico"):
            if field in row: row[field] = dinheiro(row[field])
        if "data_inicio" in row: row["data_inicio"] = data_iso(row["data_inicio"])
        result.append(row)
    return result


def carregar_planilha(caminho):
    import sys
    from pathlib import Path
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "python"))
    from analyze import read_rows
    headers, rows = read_rows(caminho)
    return [dict(zip(headers, row)) for row in rows]


# ---------------------------------------------------------------------------
# ETL da Aula 12: recebe as linhas já validadas pelo Spring (POST /etl).
# ---------------------------------------------------------------------------
from dataclasses import dataclass, field

SEGMENTOS = {
    "ind": "Indústria", "industria": "Indústria", "industrial": "Indústria",
    "com": "Comércio", "comercio": "Comércio", "comercial": "Comércio", "varejo": "Varejo",
    "serv": "Serviços", "servico": "Serviços", "servicos": "Serviços",
    "saude": "Saúde", "tec": "Tecnologia", "tecnologia": "Tecnologia", "ti": "Tecnologia",
    "educacao": "Educação", "agro": "Agronegócio", "agronegocio": "Agronegócio",
    "logistica": "Logística", "construcao": "Construção",
}
CAMPOS_ETL = ("codigo_cliente", "nome_cliente", "consultor", "segmento", "nivel_cliente",
              "faturamento_anual", "servicos_contratados", "data_contratacao", "cidade", "uf")
MINUSCULAS = {"da", "de", "do", "das", "dos", "e"}


@dataclass
class ResultadoTransformacao:
    """Tabela limpa, linhas recusadas com o motivo e quantas correções foram feitas."""
    tratado: object
    rejeitadas: list = field(default_factory=list)
    avisos: list = field(default_factory=list)
    correcoes: int = 0
    padronizacoes: dict = field(default_factory=dict)
    segmentos_originais: list = field(default_factory=list)


def snake_case(nome):
    nome = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", str(nome))
    return chave(nome)


def limpar_texto(valor):
    if valor is None:
        return ""
    texto = str(valor)
    return "" if texto.lower() in ("nan", "none", "nat") else " ".join(texto.split())


def titulo(valor):
    palavras = limpar_texto(valor).lower().split()
    return " ".join(p if i and p in MINUSCULAS else p[:1].upper() + p[1:] for i, p in enumerate(palavras))


def padronizar_segmento(valor):
    texto = limpar_texto(valor)
    return SEGMENTOS.get(chave(texto), titulo(texto)) if texto else ""


def padronizar_servicos(valor):
    vistos = {}
    for parte in re.split(r"[;|]", limpar_texto(valor)):
        nome = limpar_texto(parte)
        if nome and chave(nome) not in vistos:
            vistos[chave(nome)] = nome[:1].upper() + nome[1:]
    return list(vistos.values())


def parse_numero(valor):
    if isinstance(valor, bool):
        return None
    if isinstance(valor, (int, float)):
        return float(valor) if math.isfinite(valor) else None
    texto = limpar_texto(valor)
    return dinheiro(texto) if texto else None


def parse_data(valor):
    if isinstance(valor, datetime):
        return valor.date()
    if isinstance(valor, date):
        return valor
    texto = limpar_texto(valor)
    if not texto:
        return None
    # "2025-01-15T00:00:00" (vindo do JSON) vira só a parte da data.
    iso = data_iso(texto[:10] if re.match(r"\d{4}-\d{2}-\d{2}", texto) else texto)
    return date.fromisoformat(iso) if iso else None


def tratar(bruto):
    """Função principal: recebe o DataFrame bruto e devolve a tabela limpa."""
    import pandas as pd
    bruto = bruto.rename(columns={c: ALIAS_ETL.get(snake_case(c), snake_case(c)) for c in bruto.columns})
    linhas, rejeitadas, avisos, correcoes, codigos = [], [], [], 0, set()
    padronizacoes = {"segmento": 0, "consultor": 0, "nivel_cliente": 0}
    segmentos_originais = {}
    for posicao, original in enumerate(bruto.to_dict(orient="records")):
        linha_origem = original.get("linha_origem") or posicao + 2
        limpo = {
            "codigo_cliente": limpar_texto(original.get("codigo_cliente")).upper(),
            "nome_cliente": limpar_texto(original.get("nome_cliente")),
            "consultor": titulo(original.get("consultor")),
            "segmento": padronizar_segmento(original.get("segmento")),
            "nivel_cliente": limpar_texto(original.get("nivel_cliente")).upper(),
            "faturamento_anual": parse_numero(original.get("faturamento_anual")),
            "servicos": padronizar_servicos(original.get("servicos_contratados")),
            "data_contratacao": parse_data(original.get("data_contratacao")),
            "cidade": titulo(original.get("cidade")) or None,
            "uf": limpar_texto(original.get("uf")).upper() or None,
            "linha_origem": int(linha_origem),
        }
        motivos = []
        if not limpo["codigo_cliente"]: motivos.append("código do cliente vazio")
        elif limpo["codigo_cliente"] in codigos: motivos.append("código do cliente repetido no lote")
        if not limpo["nome_cliente"]: motivos.append("nome do cliente vazio")
        if not limpo["consultor"]: motivos.append("consultor vazio")
        if not limpo["segmento"]: motivos.append("segmento vazio")
        if limpo["nivel_cliente"] not in ("A", "B", "C"): motivos.append("nível deve ser A, B ou C")
        if limpo["faturamento_anual"] is None or limpo["faturamento_anual"] < 0: motivos.append("faturamento anual inválido")
        if not limpo["servicos"]: motivos.append("nenhum serviço contratado")
        if limpo["data_contratacao"] is None: motivos.append("data de contratação inválida")
        if limpo["uf"] and not re.fullmatch(r"[A-Z]{2}", limpo["uf"]): motivos.append("UF deve ter 2 letras")
        if motivos:
            rejeitadas.append({"linha": limpo["linha_origem"], "codigoCliente": limpo["codigo_cliente"], "motivo": "; ".join(motivos)})
            continue
        codigos.add(limpo["codigo_cliente"])
        if not limpo["cidade"]:
            avisos.append({"linha": limpo["linha_origem"], "codigoCliente": limpo["codigo_cliente"], "aviso": "cidade não informada"})
        for campo, valor in (("consultor", limpo["consultor"]), ("segmento", limpo["segmento"]),
                             ("nivel_cliente", limpo["nivel_cliente"]), ("nome_cliente", limpo["nome_cliente"])):
            if limpar_texto(original.get(campo)) != valor:
                correcoes += 1
                if campo in padronizacoes: padronizacoes[campo] += 1
        par = (limpar_texto(original.get("segmento")), limpo["segmento"])
        segmentos_originais[par] = segmentos_originais.get(par, 0) + 1
        limpo["mes"] = limpo["data_contratacao"].strftime("%Y-%m")
        limpo["data_contratacao"] = limpo["data_contratacao"].isoformat()
        linhas.append(limpo)
    colunas = ["codigo_cliente", "nome_cliente", "consultor", "segmento", "nivel_cliente", "faturamento_anual",
               "servicos", "data_contratacao", "mes", "cidade", "uf", "linha_origem"]
    segmentos = [{"original": a, "padronizado": b, "ocorrencias": n}
                 for (a, b), n in sorted(segmentos_originais.items())]
    return ResultadoTransformacao(pd.DataFrame(linhas, columns=colunas), rejeitadas, avisos, correcoes,
                                  padronizacoes, segmentos)


ALIAS_ETL = {"codigo": "codigo_cliente", "codigo_cti": "codigo_cliente", "cliente": "nome_cliente",
             "nome": "nome_cliente", "nivel": "nivel_cliente", "faturamento": "faturamento_anual",
             "servicos": "servicos_contratados", "data_inicio": "data_contratacao"}
