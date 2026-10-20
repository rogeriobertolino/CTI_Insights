import api from './Api.js'

export const recursosComerciais = Object.freeze({
  consultores: { base: '/v1/consultores', lista: '/v1/consultores' },
  clientes: { base: '/v1/clientes', lista: '/v1/clientes/pagina' },
  servicos: { base: '/v1/servicos', lista: '/v1/servicos' },
  contratos: { base: '/v1/contratos', lista: '/v1/contratos' },
  insights: { base: '/v1/insights', lista: '/v1/insights' },
  telemetrias: { lista: '/admin/telemetrias' },
})
function recurso(nome, escrita = false) {
  const config = recursosComerciais[nome]
  if (!config || (escrita && !config.base)) throw new Error('Recurso não permite esta operação.')
  return config
}
export async function listarComercial(nome, pagina = 0, filtros = {}) {
  return (await api.get(recurso(nome).lista, { params: { ...filtros, pagina, tamanho: 25 } })).data
}
export async function salvarComercial(nome, dados, id = null) {
  const base = recurso(nome, true).base
  return (await (id === null ? api.post(base, dados) : api.put(base + '/' + id, dados))).data
}
export async function excluirComercial(nome, id) {
  await api.delete(recurso(nome, true).base + '/' + id)
}
