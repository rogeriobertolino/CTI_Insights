import { criarCliente } from './Api.js'
import { obterToken, salvarToken, limparToken } from './token.js'

export const authClient = criarCliente('/api/auth')
export function authError(error) {
  if ([502, 503, 504].includes(error.response?.status) || error.code === 'ERR_NETWORK') {
    return 'Não foi possível conectar à API. Verifique se o backend está em execução e tente novamente.'
  }
  return error.response?.data?.message || error.message || 'Não foi possível conectar ao servidor.'
}
export async function cadastrarUsuario(form) {
  return (await authClient.post('/cadastro', {
    nome: form.name.trim(), email: form.email.trim().toLowerCase(), senha: form.password,
  })).data
}
export async function entrar(email, senha) {
  const { data } = await authClient.post('/login', { email: email.trim().toLowerCase(), senha })
  if (!data?.token || data.tipo !== 'Bearer' || !data.usuario) throw new Error('A API não retornou um acesso válido.')
  salvarToken(data.token)
  return data.usuario
}
export async function usuarioAtual() {
  if (!obterToken()) return null
  return (await authClient.get('/me')).data
}
// Sair também funciona offline. O JWT removido permanece válido no servidor até expirar.
export async function sair() { limparToken() }
