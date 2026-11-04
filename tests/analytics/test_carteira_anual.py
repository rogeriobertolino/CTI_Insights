import json
import sys
import unittest
import tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'python'))
sys.path.insert(0,str(ROOT/'analytics-python'))
from analyze import analyze
from carteira_anual import processar_anual, FIELDS

class CarteiraAnualTest(unittest.TestCase):
    def test_amostra_oficial(self):
        result=analyze(ROOT/'sample-data/dados.xlsx')
        a=result['analytics']
        self.assertEqual((a['inputRows'],a['validRows'],a['invalidRows']),(38,32,6))
        self.assertEqual(a['revenuePeriod'],'anual')
        self.assertEqual(a['statistics']['overall']['mean'],2261250)
        self.assertEqual(a['statistics']['overall']['median'],1470000)
        self.assertEqual(a['indicators']['contractCount'],61)
        self.assertEqual(len(a['statistics']['byConsultant']),4)
        self.assertEqual(len(a['statistics']['bySegment']),6)
        self.assertEqual(len(a['services']),6)
        self.assertEqual(len(a['topClients']),5)
        self.assertTrue(a['correlation']['available'])
        self.assertTrue(a['charts']['revenueHistogram'])
        self.assertTrue(a['charts']['revenueBySegment'])
        self.assertTrue(a['charts']['revenueServices'])
        self.assertEqual(result['totalRows'],32)
        json.dumps(result,allow_nan=False)

    def test_exportacao_anual_gera_quatro_graficos_e_csvs(self):
        from analise_cti import executar
        with tempfile.TemporaryDirectory() as folder:
            output=Path(folder)
            executar(ROOT/'sample-data/dados.xlsx',output)
            self.assertEqual(len(list((output/'graficos').glob('*.png'))),4)
            for name in ('top_clientes','estatistica_consultor','servicos','correlacao'):
                self.assertTrue((output/'saidas'/f'{name}.csv').is_file())

    def test_zero_data_invalida_segmento_e_duplicata(self):
        row=dict(zip(FIELDS,['X','Empresa','ANA SOUZA','IND.','a',0,'Internet;Firewall','2026-01-01']))
        result=processar_anual([row,{**row,'data_contratacao':'31/02/2026'}],list(FIELDS))
        self.assertEqual(result['validRows'],1)
        self.assertEqual(result['validRecords'][0]['consultor'],'Ana Souza')
        self.assertIsNone(result['indicators']['activeContracts'])
        self.assertEqual(result['indicators']['totalRevenue'],0)
        self.assertFalse(result['correlation']['available'])
        with self.assertRaises(ValueError): processar_anual([row],['codigo_cliente'])

if __name__=='__main__': unittest.main()
