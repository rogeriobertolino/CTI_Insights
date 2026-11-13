"""Testes do ETL da Aula 12 (sem banco): python -m unittest discover -s tests/analytics"""
import json
import sys
import unittest
from pathlib import Path
from unittest import mock

import pandas as pd

PASTA = Path(__file__).resolve().parents[2] / "analytics-python"
sys.path.insert(0, str(PASTA))

from analise import calcular
from insights import brl
from limpeza import padronizar_segmento, padronizar_servicos, parse_data, parse_numero, titulo, tratar


def planilha_da_aula():
    import openpyxl
    aba = openpyxl.load_workbook(PASTA.parent / "sample-data" / "dados.xlsx", read_only=True)["upload_clientes"]
    linhas = list(aba.iter_rows(values_only=True))
    return [dict(zip(linhas[0], r)) | {"linha_origem": i + 2} for i, r in enumerate(linhas[1:])]


class LimpezaTest(unittest.TestCase):
    def test_funcoes_da_tabela_do_slide(self):
        self.assertEqual(titulo("  ana   SOUZA "), "Ana Souza")
        self.assertEqual(titulo("maria da silva"), "Maria da Silva")
        self.assertEqual(padronizar_segmento("IND."), "Indústria")
        self.assertEqual(padronizar_segmento("Comercio"), "Comércio")
        self.assertEqual(padronizar_segmento("SERVIÇOS"), "Serviços")
        self.assertEqual(parse_numero("R$ 1.850.000,00"), 1850000.0)
        self.assertEqual(parse_numero(420000), 420000.0)
        self.assertIsNone(parse_numero("abc"))
        self.assertEqual(parse_data("15/01/2025").isoformat(), "2025-01-15")
        self.assertEqual(parse_data("2025-01-15T00:00:00").isoformat(), "2025-01-15")
        self.assertEqual(padronizar_servicos("Backup; firewall;Backup"), ["Backup", "Firewall"])

    def test_aceita_camel_case_do_java(self):
        amostra = json.loads((PASTA / "amostras" / "clientes.json").read_text(encoding="utf-8"))
        res = tratar(pd.DataFrame(amostra))
        self.assertEqual(len(res.tratado), 4)
        self.assertEqual(res.tratado.loc[0, "consultor"], "Ana Souza")
        self.assertEqual(res.tratado.loc[1, "faturamento_anual"], 780000.0)
        self.assertEqual(res.tratado.loc[1, "nivel_cliente"], "B")
        self.assertEqual(res.tratado.loc[2, "data_contratacao"], "2025-02-15")
        self.assertEqual(res.avisos[0]["aviso"], "cidade não informada")


class AnaliseTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        linhas = planilha_da_aula()
        cls.res = tratar(pd.DataFrame(linhas))
        cls.resultado = calcular(cls.res, len(linhas))

    def test_numeros_esperados_no_slide(self):
        resumo = self.resultado["resumo"]
        self.assertEqual((resumo["linhasRecebidas"], resumo["clientes"], resumo["contratos"]), (38, 32, 61))
        self.assertEqual(resumo["media"], 2261250.0)
        self.assertEqual(len(self.res.rejeitadas), 6)
        self.assertEqual([r["linha"] for r in self.res.rejeitadas], [32, 33, 34, 37, 38, 39])

    def test_json_do_dashboard_tem_todas_as_chaves(self):
        for chave in ("resumo", "porSegmento", "porNivel", "porConsultor", "porServico", "porMes", "topClientes",
                      "moda", "correlacao", "testes", "insights"):
            self.assertIn(chave, self.resultado)
        self.assertEqual([n["nivel"] for n in self.resultado["porNivel"]], ["A", "B", "C"])
        self.assertEqual(len(self.resultado["topClientes"]), 5)
        json.dumps(self.resultado)  # precisa caber em estatistica_lote.resultado

    def test_frases(self):
        self.assertEqual(brl(1500), "R$ 1.500,00")
        self.assertTrue(any("Internet Dedicada" in f for f in self.resultado["insights"]))

    def test_contrato_da_aula_preserva_campos_anteriores(self):
        r = self.resultado
        self.assertEqual(r["resumo"]["totalClientes"], 32)
        self.assertEqual(r["resumo"]["faturamentoTotal"], r["resumo"]["total"])
        self.assertEqual(r["qualidade"]["linhasInvalidas"], 6)
        self.assertEqual(sum(m["clientes"] for m in r["porMes"]), 32)
        self.assertEqual(sum(m["contratos"] for m in r["porMes"]), 61)
        json.dumps(r, allow_nan=False)

    def test_mes_sem_movimento_e_cliente_com_varios_servicos(self):
        amostra = json.loads((PASTA / "amostras" / "clientes.json").read_text(encoding="utf-8"))[:2]
        amostra[0]["dataContratacao"] = "2025-01-15"
        amostra[1]["dataContratacao"] = "2025-03-15"
        r = calcular(tratar(pd.DataFrame(amostra)), 2)
        self.assertEqual([m["clientes"] for m in r["porMes"]], [1, 0, 1])
        self.assertEqual([m["acumulado"] for m in r["porMes"]], [1, 1, 2])
        self.assertEqual(r["porMes"][1]["faturamento"], 0)


class ApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        from fastapi.testclient import TestClient
        import api
        cls.api = api
        cls.cliente = TestClient(api.app)

    def test_saude_e_testar(self):
        self.assertEqual(self.cliente.get("/saude").json(), {"status": "ok"})
        amostra = json.loads((PASTA / "amostras" / "clientes.json").read_text(encoding="utf-8"))
        corpo = self.cliente.post("/testar", json=amostra).json()
        self.assertEqual(corpo["resultado"]["resumo"]["clientes"], 4)
        self.assertEqual(corpo["resumo"]["totalClientes"], 4)

    def test_etl_confere_chave_e_campos(self):
        pedido = {"loteId": 1, "recebidas": 1, "linhas": [{"codigoCliente": "X"}]}
        with mock.patch.dict("os.environ", {"ETL_API_KEY": "segredo"}):
            self.assertEqual(self.cliente.post("/etl", json=pedido).status_code, 401)
            self.assertEqual(self.cliente.post("/etl", json={"linhas": []}, headers={"X-ETL-Key": "segredo"}).status_code, 422)
            with mock.patch.object(self.api, "obter_engine"), \
                 mock.patch.object(self.api, "processar_lote", return_value=True) as processar:
                resposta = self.cliente.post("/etl", json=pedido, headers={"X-ETL-Key": "segredo"})
        self.assertEqual(resposta.json(), {"ok": True, "loteId": 1})
        self.assertEqual(processar.call_args.args[1:4], (1, [{"codigoCliente": "X"}], 1))

    def test_etl_sem_id_reserva_lote_pendente(self):
        from banco import criar_lote, reservar_lote
        engine = mock.MagicMock()
        conn = engine.begin.return_value.__enter__.return_value
        conn.execute.return_value.scalar_one.return_value = 7
        conn.execute.return_value.rowcount = 1
        self.assertEqual(criar_lote(engine, "aula.xlsx", 1), 7)
        self.assertIn("'PENDENTE'", str(conn.execute.call_args.args[0]))
        self.assertTrue(reservar_lote(engine, 7))
        with mock.patch.object(self.api, "obter_engine", return_value=engine), \
             mock.patch.object(self.api, "criar_lote", return_value=7), \
             mock.patch.object(self.api, "processar_lote", return_value=True) as processar, \
             mock.patch.dict("os.environ", {"ETL_API_KEY": ""}):
            resposta = self.cliente.post("/etl", json={"recebidas": 1, "linhas": [{"codigoCliente": "X"}]})
        self.assertEqual(resposta.json(), {"ok": True, "loteId": 7})
        self.assertEqual(processar.call_args.args[1], 7)


if __name__ == "__main__":
    unittest.main()
