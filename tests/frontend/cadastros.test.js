import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createPinia, setActivePinia } from 'pinia'
import api from '../../src/services/Api.js'
import { salvarToken, limparToken } from '../../src/services/token.js'
import { useCadastrosStore } from '../../src/stores/cadastros.js'

function setup(t, handler) {
  setActivePinia(createPinia())
  salvarToken('jwt-admin')
  const previous = api.defaults.adapter
  api.defaults.adapter = handler
  t.after(() => { api.defaults.adapter = previous; limparToken() })
  return useCadastrosStore()
}
const result = (config, data) => ({ config, data, status: 200, headers: { 'content-type': 'application/json' } })
const page = content => ({ content, totais: { PENDENTE: content.length, APROVADO: 0, RECUSADO: 0 }, totalElements: content.length, totalPages: content.length ? 1 : 0, number: 0 })

test('lista pendentes com Bearer e aprova enviando apenas decisão', async t => {
  let approved = false
  const store = setup(t, async config => {
    assert.equal(config.headers.get('Authorization'), 'Bearer jwt-admin')
    if (config.method === 'patch') {
      assert.equal(config.url, '/admin/cadastros/7')
      assert.deepEqual(JSON.parse(config.data), { decisao: 'APROVADO' })
      approved = true
      return result(config, { id: 7, nome: 'Aluno', statusCadastro: 'APROVADO' })
    }
    assert.equal(config.params.status, 'PENDENTE')
    return result(config, page(approved ? [] : [{ id: 7, nome: 'Aluno', statusCadastro: 'PENDENTE' }]))
  })
  await store.carregar()
  assert.equal(store.usuarios.length, 1)
  await store.revisar(7, 'APROVADO')
  assert.equal(store.usuarios.length, 0)
  assert.match(store.mensagem, /aprovado/)
  assert.equal(store.revisando, null)
})
test('recusa não pode ser enviada duas vezes durante a revisão', async t => {
  let release, mutations = 0
  const store = setup(t, async config => {
    if (config.method === 'patch') {
      mutations++
      await new Promise(resolve => { release = resolve })
      return result(config, { id: 8, nome: 'Aluno' })
    }
    return result(config, page([]))
  })
  const pending = store.revisar(8, 'RECUSADO')
  while (!release) await new Promise(resolve => setTimeout(resolve, 0))
  await store.revisar(8, 'RECUSADO')
  release(); await pending
  assert.equal(mutations, 1)
  assert.match(store.mensagem, /recusado/)
})
test('decisão concorrente do servidor atualiza lista e mostra conflito', async t => {
  const store = setup(t, async config => {
    if (config.method === 'patch') {
      const error = new Error('Conflict')
      error.config = config; error.response = { status: 409, data: { message: 'Cadastro já analisado.' } }
      throw error
    }
    return result(config, page([]))
  })
  await store.revisar(9, 'APROVADO')
  assert.equal(store.revisando, null)
  assert.match(store.erro, /já analisado/)
})
test('403 de permissão mantém token; cadastro bloqueado limpa token', async t => {
  const store = setup(t, async config => {
    const error = new Error('Forbidden')
    error.config = config; error.response = { status: 403, data: { message: 'Somente administrador.' } }
    throw error
  })
  await store.carregar()
  const { obterToken } = await import('../../src/services/token.js')
  assert.equal(obterToken(), 'jwt-admin')
  api.defaults.adapter = async config => {
    const error = new Error('Forbidden')
    error.config = config; error.response = { status: 403, data: { code: 'CADASTRO_NAO_APROVADO' } }
    throw error
  }
  await store.carregar()
  assert.equal(obterToken(), null)
})

test('sair durante uma aprovação não restaura dados administrativos antigos', async t => {
  let release
  const store = setup(t, async config => {
    if (config.method !== 'patch') assert.fail('Não deve consultar dados depois de sair')
    await new Promise(resolve => { release = resolve })
    return result(config, { id: 10, nome: 'Solicitante' })
  })
  const pending = store.revisar(10, 'APROVADO')
  while (!release) await new Promise(resolve => setTimeout(resolve, 0))
  store.limpar()
  release(); await pending
  assert.equal(store.mensagem, '')
  assert.equal(store.usuarios.length, 0)
  assert.equal(store.revisando, null)
})
