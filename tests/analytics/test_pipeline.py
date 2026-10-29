import sys
import tempfile
import unittest
import json
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "analytics-python"))
from limpeza import limpar_dados, data_iso, REQUIRED
from pipeline import executar_etl
from estatistica import resumo, testes_estatisticos
from analise_cti import executar
from openpyxl import Workbook

class PipelineTests(unittest.TestCase):
    def row(self, **changes):
        return dict(codigo_cliente="CLI001", cliente=" Empresa Alfa ", consultor=" Maria ", segmento="IND.", nivel="a", faturamento_mensal="0", servico="Internet", valor_servico="0", status_contrato="pendente", data_inicio="29/02/2024", **{}) | changes
    def test_cleaning_validation_and_invalid_original_values(self):
        records=[self.row(),self.row(codigo_cliente="CLI002",valor_servico="-1",data_inicio="31/02/2026")]
        result=executar_etl(records,list(REQUIRED))
        self.assertEqual((result["validRows"],result["invalidRows"]),(1,1))
        self.assertEqual(result["validRecords"][0]["data_inicio"],"2024-02-29")
        self.assertEqual(result["validRecords"][0]["segmento"],"Indústria")
        self.assertEqual(result["indicators"]["totalRevenue"],0)
        self.assertIsNone(result["invalidRecords"][0]["data_inicio"])
        self.assertEqual(result["invalidRecords"][0]["original"]["data_inicio"],"31/02/2026")
    def test_duplicate_codes_both_invalid_and_missing_columns(self):
        result=executar_etl([self.row(),self.row(codigo_cliente="cli001")],list(REQUIRED))
        self.assertEqual(result["validRows"],0)
        result=executar_etl([{"codigo_cliente":"1","status_contrato":"ATIVO"}], ["codigo_cliente","status_contrato"])
        self.assertEqual(result["invalidRows"],1)
    def test_descriptive_sample_statistics_and_small_constant_samples(self):
        stats=resumo([10,20,30,40])
        self.assertEqual(stats["variance"],500/3)
        self.assertEqual(stats["q1"],17.5)
        self.assertEqual(stats["range"],30)
        self.assertIsNone(resumo([0])["variance"])
        for values in ([],[1],[1,1,1]):
            self.assertFalse(testes_estatisticos([{"faturamento_mensal":v} for v in values])["shapiroWilk"]["available"])
    def test_scientific_tests_and_sparse_contingency_guard(self):
        rows=[{"segmento":"A" if i%2 else "B","nivel":"A" if i%3 else "B","faturamento_mensal":i} for i in range(60)]
        tests=testes_estatisticos(rows)
        self.assertTrue(tests["shapiroWilk"]["available"])
        self.assertTrue(tests["chiSquare"]["expectedAtLeastFive"])
        sparse=testes_estatisticos(rows[:4])["chiSquare"]
        self.assertFalse(sparse["expectedAtLeastFive"])
        self.assertIsNone(sparse["rejectNull"])
    def test_cli_exports_csv_json_and_four_png_files(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/"base.xlsx"
            workbook=Workbook(); workbook.active.title="dados_importacao"
            workbook.active.append(list(REQUIRED))
            workbook.active.append([self.row()[c] for c in REQUIRED])
            workbook.save(path); workbook.close()
            result=executar(path,Path(folder)/"export")
            self.assertEqual(result["analytics"]["validRows"],1)
            self.assertEqual(len(list((Path(folder)/"export/graficos").glob("*.png"))),4)
            self.assertTrue((Path(folder)/"export/saidas/dados_validos.csv").read_bytes().startswith(b"\xef\xbb\xbf"))
            json.loads((Path(folder)/"export/saidas/resultado.json").read_text(encoding="utf-8"))

if __name__ == "__main__": unittest.main()
