import assert from 'node:assert/strict'
import { test } from 'node:test'
import api from '../../src/services/Api.js'
import { salvarToken, limparToken } from '../../src/services/token.js'
import { listarComercial, salvarComercial, excluirComercial } from '../../src/services/comercial.js'
import { formularioComercial, payloadComercial } from '../../src/utils/cadastrosComerciais.js'

test('serviços comerciais usam paginação, Bearer e contratos de CRUD', async t => {
  const previous = api.defaults.adapter
  salvarToken('jwt-admin')
  t.after(() => { api.defaults.adapter = previous; limparToken() })
  const calls = []
  api.defaults.adapter = async config => {
    assert.equal(config.headers.get('Authorization'), 'Bearer jwt-admin')
    calls.push(config)
    return { config, data: { content: [] }, status: 200, headers: { 'content-type': 'application/json' } }
  }
  await listarComercial('clientes', 2, { segmento: 'Serviços' })
  assert.equal(calls[0].url, '/v1/clientes/pagina')
  assert.deepEqual(calls[0].params, { segmento: 'Serviços', pagina: 2, tamanho: 25 })
  await salvarComercial('servicos', { nome: 'Teste' })
  assert.equal(calls[1].method, 'post')
  await salvarComercial('contratos', { valor: 100 }, 7)
  assert.equal(calls[2].url, '/v1/contratos/7')
  await excluirComercial('insights', 3)
  assert.equal(calls[3].method, 'delete')
})
test('auditoria é somente leitura e recurso desconhecido não gera requisição', async () => {
  await assert.rejects(salvarComercial('telemetrias', {}), /não permite/)
  await assert.rejects(listarComercial('desconhecido'), /não permite/)
})
test('formulários preservam zero, normalizam IDs e removem campos fora do DTO', () => {
  const form = formularioComercial('clientes', { codigoCti: ' A ', consultorId: 3, faturamentoAnual: 0, id: 99 })
  const payload = payloadComercial('clientes', form)
  assert.equal(payload.codigoCti, 'A')
  assert.equal(payload.consultorId, 3)
  assert.equal(payload.faturamentoAnual, 0)
  assert.equal(payload.email, null)
  assert.equal(payload.id, undefined)
  const contract = payloadComercial('contratos', { clienteId: '2', servicoId: '3', dataInicio: '2026-10-19', dataFim: '', valor: '12.34', status: 'ATIVO' })
  assert.equal(contract.clienteId, 2)
  assert.equal(contract.dataFim, null)
  assert.equal(contract.valor, 12.34)
})
