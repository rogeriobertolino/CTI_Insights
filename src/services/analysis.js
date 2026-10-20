import { criarCliente } from './Api.js'

export const analysisClient = criarCliente(import.meta.env?.VITE_API_URL || '/api/v1', 150000)
analysisClient.interceptors.response.use(response => {
  if (!/application\/(?:[\w.-]+\+)?json/i.test(response.headers['content-type'] || '')) {
    throw new Error('A API não retornou dados válidos. Verifique se o backend está em execução.')
  }
  return response
}, error => {
  if (error.response?.status === 401) throw new Error('Entre na sua conta para continuar.')
  throw new Error(error.response?.data?.message || (error.code === 'ECONNABORTED'
    ? 'O processamento excedeu o tempo limite. Confira o último resultado antes de tentar novamente.'
    : 'Não foi possível conectar à API. Verifique se o backend está em execução e tente novamente.'))
})
export async function enviarPlanilha(arquivo) {
  const body = new FormData()
  body.append('file', arquivo)
  return (await analysisClient.post('/imports', body)).data
}
export async function buscarUltimaAnalise() {
  return (await analysisClient.get('/dashboard/latest')).data
}
