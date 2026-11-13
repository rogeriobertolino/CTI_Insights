import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { createPinia, setActivePinia } from 'pinia'
import { useUploadStore } from '../../src/stores/uploadStore.js'
import { uploadsClient } from '../../src/services/uploads.js'
import { limparToken, salvarToken } from '../../src/services/token.js'

// A planilha oficial da Aula 12: 38 linhas, 32 válidas, 6 com erro.
function dadosXlsx() {
  const bytes = readFileSync(new URL('../../sample-data/dados.xlsx', import.meta.url))
  const buffer = bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength)
  return { name: 'dados.xlsx', size: bytes.byteLength, arrayBuffer: async () => buffer }
}

test('dados.xlsx: o navegador separa 32 válidas e monta o JSON do ClienteDTO', async () => {
  setActivePinia(createPinia())
  const store = useUploadStore()
  assert.equal(await store.importarArquivo(dadosXlsx()), true, store.erroArquivo)
  assert.equal(store.totalRegistros, 38)
  assert.equal(store.totalComErro, 6)
  const linhasComErro = [...new Set(store.erros.map(e => e.linha))]
  assert.deepEqual(linhasComErro, [32, 33, 34, 37, 38, 39])

  const pedido = store.prepararPedidoEtl()
  assert.equal(pedido.recebidas, 38)
  assert.equal(pedido.invalidas, 6)
  assert.equal(pedido.linhas.length, 32)
  assert.deepEqual(pedido.linhas[0], {
    linha: 2, codigoCliente: 'CTI001', nomeCliente: 'Metalúrgica Horizonte', consultor: 'Ana Souza',
    segmento: 'IND.', nivelCliente: 'A', faturamentoAnual: 1850000,
    servicosContratados: 'Internet Dedicada;Firewall', dataContratacao: '2025-01-15', cidade: 'Campinas', uf: 'SP',
  })
  // Segmento vai como veio: quem padroniza é o Python (limpeza.py).
  assert.ok(pedido.linhas.some(l => l.segmento === 'SERVIÇOS'))
})

test('envio do formato da aula usa POST /api/uploads/validadas com Bearer JWT', async t => {
  setActivePinia(createPinia())
  const store = useUploadStore()
  await store.importarArquivo(dadosXlsx())
  const original = uploadsClient.defaults.adapter
  salvarToken('jwt-aula12')
  t.after(() => { uploadsClient.defaults.adapter = original; limparToken() })
  uploadsClient.defaults.adapter = async config => {
    assert.equal(config.url, '/uploads/validadas')
    assert.equal(config.method, 'post')
    assert.equal(config.headers.get('Authorization'), 'Bearer jwt-aula12')
    assert.equal(JSON.parse(config.data).linhas.length, 32)
    return { config, status: 201, headers: { 'content-type': 'application/json' },
      data: { lote: { id: 7, status: 'PROCESSADO', linhasValidas: 32, linhasRejeitadasEtl: 0 }, rejeitadas: [] } }
  }
  const resultado = await store.enviarDados()
  assert.equal(resultado.etl, true)
  assert.equal(resultado.lote.id, 7)
  assert.equal(store.erroEnvio, '')
})
