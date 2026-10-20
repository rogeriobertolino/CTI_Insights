"""Séries para o dashboard e exportação PNG para uso acadêmico."""
from collections import Counter, defaultdict
import math

def series_graficos(rows, stats):
    months = Counter(r["data_inicio"][:7] for r in rows if r.get("data_inicio"))
    values = [r["faturamento_mensal"] for r in rows if r.get("faturamento_mensal") is not None]
    histogram = []
    if values:
        low, high = min(values), max(values)
        bins = min(10, max(1, math.ceil(math.sqrt(len(values))))) if low != high else 1
        step = (high-low)/bins if high != low else 1
        counts = [0]*bins
        for v in values: counts[min(int((v-low)/step), bins-1)] += 1
        histogram = [{"from": low+i*step, "to": low+(i+1)*step, "count": count} for i, count in enumerate(counts)]
    clients = defaultdict(list)
    for row in rows:
        if row.get("codigo_cliente"): clients[row["codigo_cliente"]].append(row)
    scatter = [{"x": len({r["servico"] for r in group if r.get("servico")}), "y": group[0].get("faturamento_mensal")} for group in clients.values() if group[0].get("faturamento_mensal") is not None and any(r.get("servico") for r in group)]
    boxes = []
    for group in stats["bySegment"]:
        if not group["count"]: continue
        nums = [r["faturamento_mensal"] for r in rows if r.get("segmento") == group["name"] and r.get("faturamento_mensal") is not None]
        iqr = group["q3"] - group["q1"]
        inside = [v for v in nums if group["q1"]-1.5*iqr <= v <= group["q3"]+1.5*iqr]
        boxes.append({**group, "lowerWhisker": min(inside), "upperWhisker": max(inside), "outliers": [v for v in nums if v not in inside]})
    return {"contractsByMonth": [{"month": m, "count": c} for m, c in sorted(months.items())], "revenueHistogram": histogram, "revenueBySegment": boxes, "revenueServices": scatter}

def salvar_graficos(rows, charts, directory):
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    directory.mkdir(parents=True, exist_ok=True)
    def save(name, title):
        plt.title(title); plt.tight_layout(); plt.savefig(directory/name, dpi=130); plt.close()
    line = charts["contractsByMonth"]
    plt.figure(); plt.plot([v["month"] for v in line], [v["count"] for v in line], marker="o"); plt.xticks(rotation=45); plt.ylabel("Contratos"); save("linha_contratacoes.png", "Contratações por mês")
    plt.figure(); plt.hist([r["faturamento_mensal"] for r in rows if r.get("faturamento_mensal") is not None], bins=10); plt.xlabel("Faturamento mensal (R$)"); plt.ylabel("Frequência"); save("histograma_faturamento.png", "Distribuição do faturamento")
    groups = defaultdict(list)
    for row in rows:
        if row.get("faturamento_mensal") is not None: groups[row.get("segmento", "Não informado")].append(row["faturamento_mensal"])
    plt.figure()
    if groups: plt.boxplot(list(groups.values()), tick_labels=list(groups)); plt.xticks(rotation=25)
    plt.ylabel("Faturamento mensal (R$)"); save("boxplot_faturamento_segmento.png", "Faturamento por segmento")
    plt.figure(); scatter = charts["revenueServices"]; plt.scatter([p["x"] for p in scatter], [p["y"] for p in scatter]); plt.xlabel("Serviços distintos por cliente"); plt.ylabel("Faturamento mensal (R$)"); save("dispersao_faturamento_servicos.png", "Faturamento e serviços")
