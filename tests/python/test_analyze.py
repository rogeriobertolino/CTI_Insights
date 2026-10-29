import contextlib
import io
import json
import tempfile
import unittest
from pathlib import Path

from openpyxl import Workbook

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "python"))

from analyze import find, main, parse_money


class WorkbookAnalysisTests(unittest.TestCase):
    def analyze(self, columns):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "workbook.xlsx"
            workbook = Workbook()
            worksheet = workbook.active
            worksheet.append(list(columns))
            for row in zip(*columns.values()):
                worksheet.append(row)
            workbook.save(path)
            workbook.close()
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                main(path)
            return json.loads(output.getvalue())

    def test_money_formats_and_single_row_statistics_are_finite(self):
        self.assertEqual(parse_money("707,553.52"),707553.52)
        self.assertEqual(parse_money("R$ 1.234,56"),1234.56)
        self.assertIsNone(parse_money("1.2.3"))
        result=self.analyze({"codigo_cliente":["1"],"nome_cliente":["Empresa 001"],"segmento":["Serviços"],"faturamento_mensal":[1234.56]})
        self.assertEqual(result["revenueStatistics"]["total"],1234.56)
        self.assertEqual(result["revenueStatistics"]["std"],0)

    def test_quartiles_and_sample_standard_deviation(self):
        result = self.analyze({"Cliente": ["A", "B", "C", "D"], "Faturamento": [10, 20, 30, 40]})
        self.assertEqual(result["revenueStatistics"]["q1"], 17.5)
        self.assertEqual(result["revenueStatistics"]["q3"], 32.5)
        self.assertEqual(result["revenueStatistics"]["std"], 12.91)
        box = result["analytics"]["charts"]["revenueBySegment"][0]
        self.assertEqual(box["name"], "Não informado")
        self.assertEqual((box["lowerWhisker"], box["upperWhisker"]), (10, 40))

    def test_exact_client_name_takes_priority_over_code_column(self):
        self.assertEqual(
            find(["Código Cliente", "Cliente"], ["razao_social", "nome_cliente", "cliente", "empresa"]),
            1,
        )

    def test_whitespace_is_normalized_before_metrics_and_records(self):
        result = self.analyze({
            "Cliente": [" Acme ", "Beta", "Gamma"],
            "Status": [" Ativo ", "Ativo", "Inativo"],
            "Segmento": ["TI ", "TI", " "],
        })
        self.assertEqual(result["activeClients"], 2)
        self.assertEqual(result["segments"], [{"name": "TI", "value": 2}])
        self.assertEqual(result["emptyCells"], 1)
        self.assertEqual(result["records"][0]["Cliente"], "Acme")

    def test_literal_na_values_are_not_discarded(self):
        result = self.analyze({"Cliente": ["NA", "NULL", "Acme"], "Status": ["Ativo"] * 3})
        self.assertEqual(result["missingKey"], 0)
        self.assertEqual([row["Cliente"] for row in result["records"]], ["NA", "NULL", "Acme"])

    def test_headers_stay_unique_after_trimming(self):
        result = self.analyze({"Cliente": ["Acme"], "Cliente ": ["Secondary"]})
        self.assertEqual(len(set(result["headers"])), 2)
        self.assertEqual(list(result["records"][0].values()), ["Acme", "Secondary"])

    def test_services_split_and_duplicates_count(self):
        result = self.analyze({
            "Código Cliente": ["1", "1", "2"],
            "Cliente": ["Acme", "Acme", "Beta"],
            "Serviço": ["Cloud / Suporte", "Cloud;Dados", "Dados"],
        })
        self.assertEqual(result["totalClients"], 2)
        self.assertEqual(result["duplicates"], 1)
        self.assertEqual(result["columns"]["clientName"], 1)
        self.assertEqual({item["name"]: item["value"] for item in result["services"]}, {
            "Cloud": 2, "Dados": 2, "Suporte": 1,
        })

    def test_segment_and_revenue_are_normalized_and_summarized(self):
        result = self.analyze({
            "Cliente": ["Acme", "Beta", "Gamma"],
            "Segmento": ["IND.", "Industria", "INDUSTRIA"],
            "Nível": ["a", "B", "C"],
            "Faturamento Anual": ["R$ 1.000,00", "2500", "3.500,00"],
        })
        self.assertEqual(result["segments"], [{"name": "Indústria", "value": 3}])
        self.assertEqual(result["levels"], [
            {"name": "A", "value": 1}, {"name": "B", "value": 1}, {"name": "C", "value": 1},
        ])
        self.assertEqual(result["revenueStatistics"]["count"], 3)
        self.assertEqual(result["revenueStatistics"]["mean"], 2333.33)


if __name__ == "__main__":
    unittest.main()
