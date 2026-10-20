"""Execução independente: exporta relatórios; envio HTTP somente com --enviar."""
import argparse
import csv
import json
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "python"))
from analyze import analyze
from graficos import salvar_graficos

def salvar_csv(path, rows):
    path.parent.mkdir(parents=True, exist_ok=True)
    fields = list(dict.fromkeys(key for row in rows for key in row))
    with path.open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields or ["sem_registros"])
        writer.writeheader()
        for row in rows:
            # Evita fórmulas ao abrir CSV exportado no Excel.
            safe = {}
            for key, value in row.items():
                text = json.dumps(value, ensure_ascii=False) if isinstance(value, (list, dict)) else value
                if isinstance(text, str) and text.lstrip().startswith(("=", "+", "-", "@")): text = "'" + text
                safe[key] = text
            writer.writerow(safe)

def executar(path, output, enviar=None):
    result = analyze(path)
    analytics = result["analytics"]
    output.mkdir(parents=True, exist_ok=True)
    rows = analytics["validRecords"]
    salvar_csv(output/"saidas/dados_validos.csv", rows)
    salvar_csv(output/"saidas/dados_invalidos.csv", analytics["invalidRecords"])
    salvar_csv(output/"saidas/estatistica_segmento.csv", analytics["statistics"]["bySegment"])
    salvar_csv(output/"saidas/estatistica_nivel.csv", analytics["statistics"]["byLevel"])
    salvar_csv(output/"saidas/indicadores_dashboard.csv", [analytics["indicators"]])
    (output/"saidas/insights.txt").write_text("\n".join(analytics["insights"]), encoding="utf-8")
    (output/"saidas/resultado.json").write_text(json.dumps(analytics, ensure_ascii=False, allow_nan=False, indent=2), encoding="utf-8")
    salvar_graficos(rows, analytics["charts"], output/"graficos")
    if enviar:
        from api_client import enviar_resultados
        enviar_resultados(enviar, analytics)
    print(f"Processamento concluído: {analytics['validRows']} válidos; {analytics['invalidRows']} inválidos. Saídas: {output}")
    return result

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("arquivo", type=Path)
    parser.add_argument("--saida", type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument("--enviar", metavar="UUID_IMPORTACAO", help="Atualiza resultado de um lote existente com JWT ADMIN em CTI_API_TOKEN")
    args = parser.parse_args()
    executar(args.arquivo, args.saida, args.enviar)
