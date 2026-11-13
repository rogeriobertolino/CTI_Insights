from collections import Counter

def indicadores(rows):
    revenues = [r["faturamento_mensal"] for r in rows if r.get("faturamento_mensal") is not None]
    statuses = [r["status_contrato"] for r in rows if r.get("status_contrato")]
    return {"totalClients": len({r.get("codigo_cliente") for r in rows if r.get("codigo_cliente")}), "totalRevenue": round(sum(revenues), 2) if revenues else None, "averageTicket": sum(revenues)/len(revenues) if revenues else None, "activeContracts": statuses.count("ATIVO") if statuses else None, "cancellationRate": statuses.count("CANCELADO")/len(statuses)*100 if statuses else None, "contractCount": len(statuses)}

def gerar_insights_contratos(rows, stats, kpis):
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


# ---------------------------------------------------------------------------
# Aula 12: frases exibidas ao lado dos gráficos do dashboard (Python puro).
# ---------------------------------------------------------------------------
def _virg(x, casas=1):
    """Troca o ponto decimal pela vírgula: 12.5 vira "12,5"."""
    return f"{x:.{casas}f}".replace(".", ",")


def brl(valor):
    """Formata dinheiro: 1500 vira "R$ 1.500,00"."""
    if valor is None:
        return "R$ -"
    texto = f"{valor:,.2f}"  # 1,500.00
    return "R$ " + texto.replace(",", "X").replace(".", ",").replace("X", ".")


def gerar_insights(resultado):
    """Recebe o JSON calculado em analise.py e devolve a lista de frases."""
    frases = []
    resumo = resultado["resumo"]
    clientes = resumo["clientes"]
    if not clientes:
        return ["Nenhum cliente válido neste lote: não há insights para mostrar."]

    if resultado["porSegmento"]:
        maior = resultado["porSegmento"][0]
        frases.append(f"O segmento {maior['segmento']} concentra {maior['clientes']} clientes "
                      f"({_virg(maior['percentual'])}% da carteira).")
        rico = max(resultado["porSegmento"], key=lambda g: g["faturamentoMedio"] or 0)
        frases.append(f"O segmento {rico['segmento']} tem o maior faturamento médio: {brl(rico['faturamentoMedio'])}.")

    total = resumo["total"] or 0
    for nivel in resultado["porNivel"]:
        if nivel["nivel"] == "A" and total:
            fatia = nivel["faturamentoTotal"] / total * 100
            frases.append(f"Clientes nível A são {_virg(nivel['percentual'])}% da carteira e respondem por "
                          f"{_virg(fatia)}% do faturamento.")

    if resultado["porServico"]:
        top = resultado["porServico"][0]
        frases.append(f"O serviço mais contratado é {top['servico']}, com {top['contratos']} contratos.")

    if resultado["porMes"]:
        pico = max(resultado["porMes"], key=lambda m: m["clientes"])
        frases.append(f"O mês com mais clientes contratantes foi {pico['mes']} ({pico['clientes']} clientes).")

    if total and resultado["topClientes"]:
        soma_top = sum(c["faturamentoAnual"] or 0 for c in resultado["topClientes"])
        frases.append(f"Os {len(resultado['topClientes'])} maiores clientes somam {_virg(soma_top / total * 100)}% "
                      f"do faturamento total ({brl(total)}).")

    if resumo["media"] is not None and resumo["mediana"] is not None and resumo["mediana"]:
        if resumo["media"] > resumo["mediana"] * 1.2:
            frases.append(f"A média ({brl(resumo['media'])}) está bem acima da mediana ({brl(resumo['mediana'])}): "
                          "poucos clientes grandes puxam o faturamento para cima.")

    correlacao = resultado.get("correlacao", {})
    if correlacao.get("disponivel"):
        frases.append(correlacao["interpretacao"])

    descartadas = resumo["linhasInvalidasNoBackend"] + resumo["linhasRejeitadasNoPython"]
    if descartadas:
        frases.append(f"{descartadas} de {resumo['linhasRecebidas']} linhas da planilha ficaram fora da análise "
                      "por erros de preenchimento: os números refletem só as linhas válidas.")
    return frases
