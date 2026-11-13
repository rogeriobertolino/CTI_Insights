"""Calcula as estatísticas do lote e devolve o JSON que o dashboard desenha."""
import math
from datetime import datetime, timezone

from insights import gerar_insights


def _num(valor, casas=2):
    if valor is None or not math.isfinite(float(valor)):
        return None
    return round(float(valor), casas)


def _resumo_serie(serie):
    """Média, mediana, desvio padrão, mínimo, máximo e quartis de uma coluna numérica."""
    serie = serie.dropna().astype(float)
    if serie.empty:
        return {"total": 0, "media": None, "mediana": None, "desvioPadrao": None,
                "minimo": None, "maximo": None, "q1": None, "q3": None}
    return {
        "total": _num(serie.sum()),
        "media": _num(serie.mean()),
        "mediana": _num(serie.median()),
        "desvioPadrao": _num(serie.std()) if len(serie) > 1 else None,
        "minimo": _num(serie.min()),
        "maximo": _num(serie.max()),
        "q1": _num(serie.quantile(0.25)),
        "q3": _num(serie.quantile(0.75)),
    }


def _agrupar(df, coluna, rotulo):
    """Clientes, soma e média de faturamento por segmento, nível ou consultor."""
    total = len(df)
    grupos = []
    for chave, g in df.groupby(coluna, sort=False):
        fat = g["faturamento_anual"].astype(float)
        grupos.append({
            rotulo: chave,
            "clientes": int(len(g)),
            "percentual": _num(len(g) / total * 100, 1),
            "faturamentoTotal": _num(fat.sum()),
            "faturamentoMedio": _num(fat.mean()),
            "faturamentoMediano": _num(fat.median()),
            "media": _num(fat.mean()),
            "mediana": _num(fat.median()),
            "total": _num(fat.sum()),
            "percentualFaturamento": _num(fat.sum() / df["faturamento_anual"].sum() * 100, 1) if df["faturamento_anual"].sum() else 0,
            "desvioPadrao": _num(fat.std()) if len(fat) > 1 else None,
            "minimo": _num(fat.min()), "maximo": _num(fat.max()),
        })
    return sorted(grupos, key=lambda item: (-item["clientes"], str(item[rotulo])))


def _contratos(df):
    """Uma linha por serviço contratado (é assim que vira a tabela contrato)."""
    return df[["codigo_cliente", "servicos", "mes"]].explode("servicos").rename(columns={"servicos": "servico"})


def _por_servico(df):
    longo = df[["codigo_cliente", "servicos", "faturamento_anual"]].explode("servicos")
    saida = [{"servico": nome, "contratos": int(g["codigo_cliente"].nunique()),
              "percentual": _num(g["codigo_cliente"].nunique() / len(df) * 100, 1),
              "faturamentoMedio": _num(g["faturamento_anual"].mean())}
             for nome, g in longo.groupby("servicos")]
    return sorted(saida, key=lambda item: (-item["contratos"], item["servico"]))


def _por_mes(df):
    """Conta clientes e preenche meses sem contratação; contratos ficam separados."""
    import pandas as pd
    if df.empty:
        return []
    meses = pd.period_range(df["mes"].min(), df["mes"].max(), freq="M").astype(str)
    grupos = df.groupby("mes").agg(clientes=("codigo_cliente", "count"), faturamento=("faturamento_anual", "sum")).reindex(meses, fill_value=0)
    contratos = _contratos(df)["mes"].value_counts()
    acumulado = grupos["clientes"].cumsum()
    return [{"mes": mes, "clientes": int(r.clientes), "faturamento": _num(r.faturamento),
             "acumulado": int(acumulado[mes]), "contratos": int(contratos.get(mes, 0))}
            for mes, r in grupos.iterrows()]


def _top_clientes(df, quantidade=5):
    top = df.sort_values("faturamento_anual", ascending=False).head(quantidade)
    return [{"codigoCliente": r.codigo_cliente, "nomeCliente": r.nome_cliente, "segmento": r.segmento,
             "nivel": r.nivel_cliente, "faturamentoAnual": _num(r.faturamento_anual),
             "codigo": r.codigo_cliente, "nome": r.nome_cliente, "faturamento": _num(r.faturamento_anual)} for r in top.itertuples()]


def _moda(serie):
    contagem = serie.value_counts()
    if contagem.empty:
        return None
    maximo = contagem.max()
    return sorted(str(v) for v, qtd in contagem.items() if qtd == maximo)[0]


def _correlacao(df):
    """Correlação de Pearson entre nº de serviços contratados e faturamento (scipy)."""
    servicos = df["servicos"].map(len).astype(float)
    fat = df["faturamento_anual"].astype(float)
    base = {"pearson": None, "p": None, "spearman": None, "spearmanP": None, "n": len(df)}
    if len(df) < 3 or servicos.nunique() < 2 or fat.nunique() < 2:
        return {**base, "disponivel": False, "motivo": "É preciso ao menos 3 clientes com variação de serviços e de faturamento."}
    try:
        from scipy.stats import pearsonr, spearmanr
        r, p = pearsonr(servicos, fat)
        rho, p_rho = spearmanr(servicos, fat)
    except Exception as erro:  # scipy ausente ou quebrado não derruba o ETL
        return {**base, "disponivel": False, "motivo": f"SciPy indisponível: {erro.__class__.__name__}"}
    forca = "forte" if abs(r) >= 0.7 else "moderada" if abs(r) >= 0.4 else "fraca"
    sentido = "positiva" if r > 0 else "negativa"
    significativa = p < 0.05
    return {
        "disponivel": True, "coeficiente": _num(r, 4), "pValor": _num(p, 6), "n": int(len(df)),
        "pearson": _num(r, 4), "p": _num(p, 6), "spearman": _num(rho, 4), "spearmanP": _num(p_rho, 6),
        "interpretacao": f"Correlação {forca} e {sentido} entre serviços e faturamento"
                         + (" (estatisticamente significativa, p < 0,05)." if significativa else " (não significativa, p ≥ 0,05)."),
    }


def _testes(df):
    """Shapiro-Wilk (normalidade do faturamento) e Qui-quadrado (segmento × nível)."""
    resultado = {}
    fat = df["faturamento_anual"].astype(float)
    try:
        from scipy.stats import chi2_contingency, shapiro
    except Exception as erro:
        motivo = f"SciPy indisponível: {erro.__class__.__name__}"
        return {"shapiroWilk": {"disponivel": False, "motivo": motivo}, "quiQuadrado": {"disponivel": False, "motivo": motivo}}
    if 3 <= len(fat) <= 5000 and fat.nunique() > 1:
        estat, p = shapiro(fat)
        resultado["shapiroWilk"] = {
            "disponivel": True, "estatistica": _num(estat, 4), "pValor": _num(p, 6),
            "interpretacao": "O faturamento não segue distribuição normal (p < 0,05): prefira a mediana."
            if p < 0.05 else "Não há evidência contra a normalidade do faturamento (p ≥ 0,05).",
        }
    else:
        resultado["shapiroWilk"] = {"disponivel": False, "motivo": "Exige de 3 a 5.000 valores com variação."}
    import pandas as pd
    tabela = pd.crosstab(df["segmento"], df["nivel_cliente"])
    if tabela.shape[0] >= 2 and tabela.shape[1] >= 2:
        estat, p, gl, esperado = chi2_contingency(tabela)
        resultado["quiQuadrado"] = {
            "disponivel": True, "estatistica": _num(estat, 4), "pValor": _num(p, 6), "grausLiberdade": int(gl),
            "amostraPequena": bool((esperado < 5).any()),
            "interpretacao": ("Há associação entre segmento e nível (p < 0,05)."
                              if p < 0.05 else "Não há evidência de associação entre segmento e nível (p ≥ 0,05).")
            + (" Atenção: frequências esperadas abaixo de 5 tornam o teste pouco confiável." if (esperado < 5).any() else ""),
        }
    else:
        resultado["quiQuadrado"] = {"disponivel": False, "motivo": "Exige ao menos 2 segmentos e 2 níveis."}
    return resultado


def calcular(res, linhas_recebidas, invalidas=0, com_aviso=0):
    """Função principal: devolve o JSON do dashboard a partir do ResultadoTransformacao."""
    df = res.tratado
    contratos = _contratos(df) if not df.empty else df
    resumo = {
        "linhasRecebidas": int(linhas_recebidas),
        "linhasInvalidasNoBackend": int(invalidas),
        "linhasRejeitadasNoPython": len(res.rejeitadas),
        "linhasComAviso": int(com_aviso) + len(res.avisos),
        "correcoesFeitas": int(res.correcoes),
        "clientes": int(len(df)),
        "contratos": int(len(contratos)),
        **_resumo_serie(df["faturamento_anual"]),
    }
    resumo.update({"totalClientes": resumo["clientes"], "faturamentoTotal": resumo["total"],
                   "consultores": int(df["consultor"].nunique()),
                   "mediaServicosPorCliente": _num(df["servicos"].map(len).mean()),
                   "coeficienteVariacao": _num(df["faturamento_anual"].std() / df["faturamento_anual"].mean(), 3)
                   if len(df) > 1 and df["faturamento_anual"].mean() else None})
    resultado = {
        "geradoEm": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "resumo": resumo,
        "porSegmento": _agrupar(df, "segmento", "segmento"),
        "porNivel": sorted(_agrupar(df, "nivel_cliente", "nivel"), key=lambda g: g["nivel"]),
        "porConsultor": _agrupar(df, "consultor", "consultor"),
        "porServico": _por_servico(df),
        "porMes": _por_mes(df),
        "topClientes": _top_clientes(df),
        "moda": {"segmento": _moda(df["segmento"]), "nivel": _moda(df["nivel_cliente"]),
                 "servico": _moda(contratos["servico"]) if len(contratos) else None},
        "correlacao": _correlacao(df),
        "testes": _testes(df),
        "rejeitadas": res.rejeitadas[:200],
    }
    resumo.update({"segmentoMaisFrequente": resultado["moda"]["segmento"],
                   "nivelMaisFrequente": resultado["moda"]["nivel"],
                   "servicoMaisContratado": resultado["moda"]["servico"]})
    resultado["qualidade"] = {"linhasRecebidas": int(linhas_recebidas), "linhasValidas": len(df),
                             "linhasInvalidas": int(linhas_recebidas) - len(df),
                             "correcoesFeitas": int(res.correcoes), "avisos": res.avisos}
    resultado["qualidade"].update({"correcoesSegmento": res.padronizacoes.get("segmento", 0),
                                  "correcoesConsultor": res.padronizacoes.get("consultor", 0),
                                  "correcoesNivel": res.padronizacoes.get("nivel_cliente", 0),
                                  "segmentosOriginais": res.segmentos_originais})
    resultado["insights"] = gerar_insights(resultado)
    return resultado
