import api from './Api.js'

export async function listarCadastros(status = 'PENDENTE', pagina = 0) {
  return (await api.get('/admin/cadastros', { params: { ...(status ? { status } : {}), pagina } })).data
}
export async function revisarCadastro(id, decisao) {
  return (await api.patch('/admin/cadastros/' + id, { decisao })).data
}
