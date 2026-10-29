import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createPinia, setActivePinia } from 'pinia'
import { authClient, cadastrarUsuario, entrar, usuarioAtual, sair } from '../../src/services/auth.js'
import { obterToken, limparToken, salvarToken } from '../../src/services/token.js'
import { useAuthStore } from '../../src/stores/auth.js'

function adapter(t, handler) {
  limparToken()
  const old = authClient.defaults.adapter
  authClient.defaults.adapter = handler
  t.after(() => { authClient.defaults.adapter = old; limparToken() })
}
function response(config, data) { return { config, data, status: 200, headers: { 'content-type': 'application/json' } } }

test('cadastro usa endpoint da aula e login JSON guarda JWT para /me', async t => {
  const user = { id: 1, nome: 'Aluno', email: 'aluno@example.com', perfil: 'USER' }
  adapter(t, async config => {
    const body = config.data ? JSON.parse(config.data) : null
    if (config.url === '/cadastro') {
      assert.deepEqual(body, { nome: 'Aluno', email: user.email, senha: 'senha1234' })
      assert.equal(config.headers.get('Authorization'), undefined)
      return response(config, user)
    }
    if (config.url === '/login') {
      assert.deepEqual(body, { email: user.email, senha: 'senha1234' })
      assert.match(config.headers.get('Content-Type'), /application\/json/)
      return response(config, { token: 'jwt-valido', tipo: 'Bearer', usuario: user })
    }
    assert.equal(config.url, '/me')
    assert.equal(config.headers.get('Authorization'), 'Bearer jwt-valido')
    assert.equal(config.withCredentials, false)
    return response(config, user)
  })
  await cadastrarUsuario({ name: ' Aluno ', email: ' ALUNO@example.com ', password: 'senha1234' })
  assert.deepEqual(await entrar(' ALUNO@example.com ', 'senha1234'), user)
  assert.equal(obterToken(), 'jwt-valido')
  assert.deepEqual(await usuarioAtual(), user)
  await sair()
  assert.equal(obterToken(), null)
  assert.equal(await usuarioAtual(), null)
})
test('token recusado limpa acesso e restauração remove usuário da store', async t => {
  adapter(t, async config => {
    const error = new Error('Unauthorized')
    error.config = config; error.response = { status: 401 }
    throw error
  })
  setActivePinia(createPinia())
  const store = useAuthStore()
  salvarToken('expirado'); store.usuario = { nome: 'Anterior' }
  await store.restaurarSessao()
  assert.equal(obterToken(), null)
  assert.equal(store.usuario, null)
})
test('resposta atrasada de outro token não apaga login recente', async t => {
  adapter(t, async config => {
    salvarToken('novo-token')
    const error = new Error('Unauthorized')
    error.config = config; error.response = { status: 401 }
    throw error
  })
  salvarToken('token-antigo')
  await assert.rejects(usuarioAtual())
  assert.equal(obterToken(), 'novo-token')
})
test('login sem token não cria acesso; sair funciona sem rede', async t => {
  adapter(t, async config => response(config, { message: 'Login realizado.' }))
  await assert.rejects(entrar('aluno@example.com', 'senha1234'), /acesso válido/)
  assert.equal(obterToken(), null)
  salvarToken('token')
  authClient.defaults.adapter = async () => { throw new Error('offline') }
  await sair()
  assert.equal(obterToken(), null)
})
