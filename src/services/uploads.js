import { criarCliente } from './Api.js'

// Fluxo da Aula 12: Vue -> Spring (/api/uploads/validadas) -> FastAPI (/etl) -> PostgreSQL.
// O tempo limite cobre a espera do Spring pelo Python (até 120 s).
export const uploadsClient = criarCliente('/api', 150000)

function mensagem(error) {
  if (error.response?.status === 401) return 'Entre na sua conta para continuar.'
  if (error.response?.data?.message) return error.response.data.message
  if (error.code === 'ECONNABORTED') return 'O processamento excedeu o tempo limite. Confira o histórico de lotes antes de enviar de novo.'
  return 'Não foi possível conectar à API. Verifique se o backend está em execução e tente novamente.'
}

async function chamar(promessa) {
  try {
    return (await promessa).data
  } catch (error) {
    throw new Error(mensagem(error))
  }
}

export function enviarLinhasValidadas(pedido) {
  return chamar(uploadsClient.post('/uploads/validadas', pedido))
}

export function listarLotes() {
  return chamar(uploadsClient.get('/uploads'))
}

export function obterDashboard(loteId) {
  return chamar(uploadsClient.get('/dashboard', { params: loteId ? { loteId } : {} }))
}
