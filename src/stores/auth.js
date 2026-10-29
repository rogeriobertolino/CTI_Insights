import { defineStore } from 'pinia'
import { useCadastrosStore } from './cadastros.js'
import { clearData } from './dataStore.js'
import { authError, entrar, sair, usuarioAtual } from '../services/auth.js'
export const useAuthStore = defineStore('auth', {
  state: () => ({ usuario: null, carregando: false, erro: '' }),
  actions: {
    async restaurarSessao() {
      try { this.usuario = await usuarioAtual() } catch { this.usuario = null }
    },
    async login(email, senha) {
      this.carregando = true; this.erro = ''
      try {
        this.usuario = await entrar(email, senha)
        clearData()
        useCadastrosStore().limpar()
        return true
      } catch (error) { this.erro = authError(error); return false }
      finally { this.carregando = false }
    },
    async logout() {
      this.carregando = true; this.erro = ''
      try { await sair(); this.usuario = null; useCadastrosStore().limpar(); return true }
      catch (error) { this.erro = authError(error); return false }
      finally { this.carregando = false }
    },
  },
})
