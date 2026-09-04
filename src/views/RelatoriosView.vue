<script setup>
import { computed, onMounted, ref } from 'vue'
import { dataStore, loadLatestAnalysis } from '../dataStore.js'
const q=ref('')
onMounted(loadLatestAnalysis)
const headers=computed(()=>dataStore.analysis?.headers||[])
const rows=computed(()=>(dataStore.analysis?.records||[]).map(record=>headers.value.map(header=>record[header]??'')))
const visibleHeaders=computed(()=>headers.value.slice(0,8))
const filtered=computed(()=>rows.value.filter(row=>row.join(' ').toLowerCase().includes(q.value.toLowerCase())))
const quote=value=>`"${String(value??'').replaceAll('"','""')}"`
const exportCsv=()=>{if(!filtered.value.length)return;const lines=[headers.value,...filtered.value].map(row=>row.map(quote).join(';')).join('\n');const blob=new Blob(['\ufeff'+lines],{type:'text/csv;charset=utf-8'});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`analise-${dataStore.analysis.fileName.replace(/\.xlsx$/i,'')}.csv`;a.click();URL.revokeObjectURL(url)}
</script>

<template>
  <section class="page">
    <div class="page-head"><div><p class="kicker">DADOS ANALISADOS</p><h1>Registros da planilha</h1><span>Pesquise e exporte o resultado do arquivo importado.</span></div><button class="btn primary" :disabled="!filtered.length" @click="exportCsv">↓ Exportar CSV</button></div>
    <div v-if="!dataStore.analysis" class="panel empty-state"><b>Nenhum arquivo disponível</b><p>Faça o upload de uma planilha para consultar seus registros.</p><RouterLink class="btn primary" to="/app/upload">Importar planilha →</RouterLink></div>
    <template v-else><div class="report-summary"><p>Total exibido<b>{{ filtered.length }}</b></p><p>Colunas identificadas<b>{{ headers.length }}</b></p><aside>● {{ dataStore.analysis.fileName }}<br><small>Lote: {{ dataStore.analysis.batchId }}</small></aside></div>
    <article class="panel report"><div class="filters"><input v-model="q" type="search" placeholder="⌕  Buscar em todos os campos"><button class="btn outline" @click="q=''">Limpar</button></div><div class="table-wrap"><table><thead><tr><th v-for="header in visibleHeaders" :key="header">{{ header }}</th></tr></thead><tbody><tr v-for="(row,index) in filtered" :key="index"><td v-for="(_,column) in visibleHeaders" :key="column">{{ row[column] || '—' }}</td></tr><tr v-if="!filtered.length"><td :colspan="visibleHeaders.length">Nenhum registro encontrado.</td></tr></tbody></table></div></article>
    <aside class="notice">ⓘ <span><b>Análise descritiva</b><p>Os resultados dependem da estrutura, dos nomes das colunas e da qualidade dos dados presentes na planilha.</p></span></aside></template>
  </section>
</template>
