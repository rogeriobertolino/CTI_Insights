from collections import Counter

def indicadores(rows):
    revenues = [r["faturamento_mensal"] for r in rows if r.get("faturamento_mensal") is not None]
    statuses = [r["status_contrato"] for r in rows if r.get("status_contrato")]
    return {"totalClients": len({r.get("codigo_cliente") for r in rows if r.get("codigo_cliente")}), "totalRevenue": round(sum(revenues), 2) if revenues else None, "averageTicket": sum(revenues)/len(revenues) if revenues else None, "activeContracts": statuses.count("ATIVO") if statuses else None, "cancellationRate": statuses.count("CANCELADO")/len(statuses)*100 if statuses else None, "contractCount": len(statuses)}

def gerar_insights(rows, stats, kpis):
    result = []
    groups = [g for g in stats["bySegment"] if g["count"]]
    if groups:
        maximum = max(g["total"] for g in groups)
        result.append("Segmento(s) com maior faturamento: " + ", ".join(g["name"] for g in groups if g["total"] == maximum) + ".")
    for field, title in (("nivel", "Nível(is) mais frequente(s)"), ("servico", "Serviço(s) mais contratado(s)")):
        counts = Counter(r[field] for r in rows if r.get(field))
        if counts:
            result.append(title + ": " + ", ".join(name for name, count in counts.items() if count == max(counts.values())) + ".")
    if kpis["cancellationRate"] is not None: result.append(f"Taxa de cancelamento: {kpis['cancellationRate']:.2f}% dos contratos desta importação.")
    return result
