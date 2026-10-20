import axios from 'axios'
import { obterToken, limparToken } from './token.js'

export function criarCliente(baseURL, timeout = 15000) {
  const origin = (import.meta.env?.VITE_BACKEND_ORIGIN || '').replace(/\/$/, '')
  const address = /^https?:\/\//i.test(baseURL) ? baseURL : origin + baseURL
  const client = axios.create({ baseURL: address, timeout, withCredentials: false })
  client.interceptors.request.use(config => {
    const publico = ['/login', '/cadastro', '/register'].includes(config.url)
    const token = obterToken()
    if (token && !publico) config.headers.set('Authorization', 'Bearer ' + token)
    return config
  })
  client.interceptors.response.use(response => response, error => {
    const token = obterToken()
    const authorization = error.config?.headers?.get?.('Authorization')
    // Uma resposta antiga não encerra um login mais recente.
    if ((error.response?.status === 401 || error.response?.data?.code === 'CADASTRO_NAO_APROVADO') && token && authorization === 'Bearer ' + token) {
      limparToken()
      globalThis.window?.dispatchEvent(new Event('cti:auth-expired'))
    }
    return Promise.reject(error)
  })
  return client
}
export default criarCliente('/api')
