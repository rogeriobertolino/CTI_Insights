<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { analysisClient } from '../services/analysis.js'
import { dataStore } from '../stores/dataStore.js'
const router = useRouter()
const history = ref(null), page = ref(0), loading = ref(false), error = ref('')
async function load() {
  loading.value = true; error.value = ''
  try { history.value = (await analysisClient.get('/imports', { params: { page: page.value, size: 25 } })).data }
  catch(e) { error.value = e.message }
  finally { loading.value = false }
}
async function open(id) {
  if (loading.value) return
  loading.value = true; error.value = ''
  try { dataStore.analysis = (await analysisClient.get('/imports/' + id)).data; dataStore.error = ''; await router.push('/app/graficos') }
  catch(e) { error.value = e.message }
  finally { loading.value = false }
}
async function move(delta) { page.value += delta; await load() }
onMounted(load)
</script>
<template><section class="page"><div class="page-head"><div><p class="kicker">ANÁLISES NO SERVIDOR</p><h1>Histórico de importações</h1><span>Consulte os resultados dos lotes salvos no banco.</span></div></div><p v-if="error" role="alert">{{ error }}</p><p v-if="loading" role="status">Carregando…</p><article class="panel"><div class="table-wrap"><table><thead><tr><th>Arquivo</th><th>Data</th><th>Registros</th><th>Status</th><th>Resultado</th></tr></thead><tbody><tr v-for="item in history?.content || []" :key="item.id"><td>{{ item.fileName }}</td><td>{{ new Date(item.createdAt).toLocaleString('pt-BR') }}</td><td>{{ item.rowCount }}</td><td>{{ item.status }}</td><td><button class="btn outline" :disabled="loading || item.status !== 'COMPLETED'" @click="open(item.id)">Ver análise</button></td></tr></tbody></table></div><p v-if="!loading && !history?.content?.length">Nenhuma importação encontrada.</p><button class="btn outline" :disabled="loading || page === 0" @click="move(-1)">Anterior</button> Página {{ page + 1 }} <button class="btn outline" :disabled="loading || !history || page + 1 >= history.totalPages" @click="move(1)">Próxima</button><button v-if="error" class="btn outline" :disabled="loading" @click="load">Tentar novamente</button></article></section></template>
