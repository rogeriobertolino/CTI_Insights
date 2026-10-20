"""Estatística amostral e testes com condições de aplicabilidade explícitas."""
import math
import statistics
from collections import defaultdict

def resumo(values):
    values = sorted(float(v) for v in values if v is not None and math.isfinite(float(v)))
    if not values: return {"count": 0}
    def q(p):
        i = (len(values) - 1) * p
        return values[math.floor(i)] + (values[math.ceil(i)] - values[math.floor(i)]) * (i - math.floor(i))
    return {"count": len(values), "total": round(math.fsum(values), 2), "mean": statistics.mean(values), "median": statistics.median(values), "modes": statistics.multimode(values), "variance": statistics.variance(values) if len(values)>1 else None, "std": statistics.stdev(values) if len(values)>1 else None, "min": values[0], "max": values[-1], "range": values[-1]-values[0], "q1": q(.25), "q2": q(.5), "q3": q(.75)}

def calcular_estatisticas(rows):
    groups = {}
    for field in ("segmento", "nivel"):
        buckets = defaultdict(list)
        for row in rows: buckets[row.get(field) or "Não informado"].append(row.get("faturamento_mensal"))
        groups[field] = [{"name": name, **resumo(values)} for name, values in sorted(buckets.items())]
    return {"overall": resumo([r.get("faturamento_mensal") for r in rows]), "bySegment": groups["segmento"], "byLevel": groups["nivel"]}

def testes_estatisticos(rows):
    values = [r["faturamento_mensal"] for r in rows if r.get("faturamento_mensal") is not None]
    sw = {"available": False, "reason": "Shapiro-Wilk exige entre 3 e 5.000 valores e variação na amostra."}
    if 3 <= len(values) <= 5000 and len(set(values)) > 1:
        from scipy.stats import shapiro
        stat, p = shapiro(values)
        sw = {"available": True, "statistic": float(stat), "pValue": float(p), "alpha": .05, "rejectNull": bool(p <= .05), "interpretation": "Rejeita a hipótese de normalidade." if p <= .05 else "Não rejeita a hipótese de normalidade; isso não comprova normalidade."}
    pairs = [(r.get("segmento"), r.get("nivel")) for r in rows if r.get("segmento") and r.get("nivel") in ("A", "B", "C")]
    segments = sorted({s for s, _ in pairs}); levels = sorted({n for _, n in pairs})
    chi = {"available": False, "reason": "Qui-quadrado exige ao menos dois segmentos e dois níveis."}
    if len(segments) >= 2 and len(levels) >= 2:
        table = [[sum(s == segment and n == level for s, n in pairs) for level in levels] for segment in segments]
        from scipy.stats import chi2_contingency
        stat, p, dof, expected = chi2_contingency(table)
        reliable = bool((expected >= 5).all())
        chi = {"available": True, "statistic": float(stat), "pValue": float(p), "degreesOfFreedom": int(dof), "alpha": .05, "expectedAtLeastFive": reliable, "rejectNull": bool(p <= .05) if reliable else None, "segments": segments, "levels": levels, "observed": table, "expected": expected.tolist(), "interpretation": ("Há evidência de associação." if p <= .05 else "Não há evidência suficiente de associação.") if reliable else "Frequências esperadas abaixo de 5: aproximação assintótica insuficiente para conclusão."}
    return {"shapiroWilk": sw, "chiSquare": chi}
