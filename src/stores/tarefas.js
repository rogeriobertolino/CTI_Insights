// src/stores/tarefas.js
import { defineStore } from 'pinia'

export const useTarefasStore = defineStore('tarefas', {

  // STATE = os dados guardados
  state: () => ({
    lista: []   // começa vazia
  }),

  // GETTERS = uma "conta" feita em cima dos dados
  getters: {
    // conta quantas tarefas existem
    total: (state) => state.lista.length
  },

  // ACTIONS = funções para mudar os dados
  actions: {
    // adiciona uma tarefa nova na lista
    adicionar(texto) {
      this.lista.push(texto)
    },
    // remove uma tarefa pelo número dela na lista (índice)
    remover(indice) {
      this.lista.splice(indice, 1)
    }
  }
})