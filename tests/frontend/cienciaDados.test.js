import test from 'node:test'
import assert from 'node:assert/strict'
import { dataContrato } from '../../src/utils/dataContrato.js'
import { csvSeguro } from '../../src/utils/exportarAnalise.js'
import { setActivePinia, createPinia } from 'pinia'
import { useUploadStore } from '../../src/stores/uploadStore.js'
import * as XLSX from 'xlsx'

test('datas comerciais validam calendário e preservam formato ISO', () => {
  assert.equal(dataContrato('29/02/2024'), '2024-02-29')
  for (const invalid of ['29/02/2025','2026-04-31','01/13/2026','texto','']) assert.equal(dataContrato(invalid), null)
})
test('CSV protege fórmulas e mantém valores numéricos e aspas', () => {
  const csv = csvSeguro([{ nome: '=HYPERLINK("url")', valor: -5 }, { nome: 'Texto; com "aspas"', valor: 0 }])
  assert.ok(csv.includes("'=HYPERLINK")); assert.ok(csv.includes('"-5"')); assert.ok(csv.includes('"Texto; com ""aspas"""'))
})
test('modelo comercial normaliza, aceita zero e rejeita contratos inválidos', async () => {
  setActivePinia(createPinia())
  const store = useUploadStore()
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet([
    ['codigo_cliente','cliente','consultor','segmento','nivel','faturamento_mensal','servico','valor_servico','status_contrato','data_inicio'],
    ['CLI001',' EMPRESA ALFA ',' Maria ','IND.','a',0,'Internet',0,'pendente','29/02/2024'],
    ['CLI002','Empresa Beta','Maria','Saúde','B',150,'Telefonia',-1,'DESCONHECIDO','31/02/2026'],
  ]), 'dados_importacao')
  const bytes = XLSX.write(workbook, { type: 'array', bookType: 'xlsx' })
  const file = new File([bytes], 'comercial.xlsx')
  assert.equal(await store.importarArquivo(file), true)
  assert.equal(store.totalValidos, 1)
  assert.equal(store.clientesValidos[0].status_contrato, 'PENDENTE')
  assert.equal(store.clientesValidos[0].data_inicio, '2024-02-29')
  assert.equal(store.clientesValidos[0].nome_cliente, 'EMPRESA ALFA')
  const output = XLSX.read(await store.prepararArquivoEnvio().arrayBuffer())
  assert.equal(output.SheetNames[0], 'dados_importacao')
  assert.equal(XLSX.utils.sheet_to_json(output.Sheets.dados_importacao).length, 1)
  assert.equal(store.dadosOriginais[0].nome, ' EMPRESA ALFA ')
})
