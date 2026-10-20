import { defineStore } from 'pinia'
import { listarCadastros, revisarCadastro } from '../services/cadastros.js'
import { authError } from '../services/auth.js'

const versoes = new WeakMap()
const sessoes = new WeakMap()
export const useCadastrosStore = defineStore('cadastros', {
  state: () => ({
    usuarios: [], totais: { PENDENTE: 0, APROVADO: 0, RECUSADO: 0 },
    filtro: 'PENDENTE', pagina: 0, totalPaginas: 0, total: 0,
    carregando: false, revisando: null, erro: '', mensagem: '',
  }),
  actions: {
    limpar() { versoes.set(this, (versoes.get(this) || 0) + 1); sessoes.set(this, (sessoes.get(this) || 0) + 1); this.$reset() },
    async carregar(filtro = this.filtro, pagina = this.pagina) {
      const versao = (versoes.get(this) || 0) + 1
      versoes.set(this, versao)
      this.filtro = filtro; this.pagina = pagina; this.carregando = true; this.erro = ''
      try {
        const data = await listarCadastros(filtro, pagina)
        if (versoes.get(this) !== versao) return
        if (data.totalElements && pagina >= data.totalPages) return await this.carregar(filtro, data.totalPages - 1)
        this.usuarios = data.content; this.totais = data.totais
        this.total = data.totalElements; this.totalPaginas = data.totalPages; this.pagina = data.number
      } catch (error) {
        if (versoes.get(this) === versao) { this.erro = authError(error); this.usuarios = [] }
      } finally { if (versoes.get(this) === versao) this.carregando = false }
    },
    async revisar(id, decisao) {
      if (this.revisando !== null || this.carregando) return
      const sessao = sessoes.get(this) || 0
      this.revisando = id; this.erro = ''; this.mensagem = ''
      try {
        const user = await revisarCadastro(id, decisao)
        if ((sessoes.get(this) || 0) !== sessao) return
        await this.carregar()
        if ((sessoes.get(this) || 0) !== sessao) return
        this.mensagem = decisao === 'APROVADO' ? 'Cadastro de ' + user.nome + ' aprovado.' : 'Cadastro de ' + user.nome + ' recusado.'
      } catch (error) {
        if ((sessoes.get(this) || 0) !== sessao) return
        if (error.response?.status === 409) await this.carregar()
        this.erro = authError(error)
      } finally { if ((sessoes.get(this) || 0) === sessao) this.revisando = null }
    },
  },
})
