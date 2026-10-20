from limpeza import limpar_dados, CONTRACT_FIELDS, chave, ALIASES
from validacao import validar_dados
from estatistica import calcular_estatisticas, testes_estatisticos
from insights import indicadores, gerar_insights
from graficos import series_graficos

def executar_etl(records, headers):
    fields = {ALIASES.get(chave(h), chave(h)) for h in headers}
    contractual = bool(fields & CONTRACT_FIELDS)
    cleaned = limpar_dados(records)
    if contractual: valid, invalid = validar_dados(cleaned, records)
    else: valid, invalid = cleaned, []
    stats = calcular_estatisticas(valid)
    kpis = indicadores(valid)
    return {"schema": "comercial-contratos-v1" if contractual else "clientes-legado", "inputRows": len(records), "validRows": len(valid), "invalidRows": len(invalid), "validRecords": valid, "invalidRecords": invalid, "statistics": stats, "statisticalTests": testes_estatisticos(valid), "indicators": kpis, "insights": gerar_insights(valid, stats, kpis), "charts": series_graficos(valid, stats)}
