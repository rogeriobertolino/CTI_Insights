const CHAVE_TOKEN = 'cti.jwt'
let tokenEmMemoria = null

// Mantém o acesso durante recargas da mesma aba, sem persistir a senha.
export function obterToken() {
  try { return globalThis.sessionStorage?.getItem(CHAVE_TOKEN) || tokenEmMemoria } catch { return tokenEmMemoria }
}
export function salvarToken(token) {
  tokenEmMemoria = token
  try { globalThis.sessionStorage?.setItem(CHAVE_TOKEN, token) } catch { /* Acesso funciona em memória quando storage está bloqueado. */ }
}
export function limparToken() {
  tokenEmMemoria = null
  try { globalThis.sessionStorage?.removeItem(CHAVE_TOKEN) } catch { /* Sem armazenamento disponível. */ }
}
