"""Normalização determinística; preserva linha e valores originais para auditoria."""
import re
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
