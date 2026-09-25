// src/components/Tarefas.vue
<template>
  <div>
    <p>Total de tarefas: {{ tarefasStore.total }}</p>

    <ul>
      <li v-for="(tarefa, indice) in tarefasStore.lista" :key="indice">
        {{ tarefa }}
        <button @click="tarefasStore.remover(indice)">Remover</button>
      </li>
    </ul>

    <input v-model="novaTarefa" placeholder="Nova tarefa" />
    <button @click="adicionarTarefa">Adicionar</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useTarefasStore } from '../stores/tarefas.js'

// pega a store para poder usar
const tarefasStore = useTarefasStore()

// campo do input
const novaTarefa = ref('')

function adicionarTarefa() {
  if (novaTarefa.value.trim() === '') return
  tarefasStore.adicionar(novaTarefa.value)  // chama a action
  novaTarefa.value = ''  // limpa o input
}
</script>
