<script setup>
import { computed } from 'vue'
import { dataStore } from '../dataStore.js'
const a=computed(()=>dataStore.analysis)
const fmt=value=>new Intl.NumberFormat('pt-BR').format(value||0)
const percent=value=>`${Number(value||0).toFixed(1)}%`
const width=(value,list)=>`${Math.max(4,value/Math.max(...list.map(x=>x.value),1)*100)}%`
</script>

<template>
  <section class="page dashboard">
    <div class="page-head"><div><p class="kicker">ANÁLISE DA PLANILHA</p><h1>Visão da carteira</h1><span>Indicadores calculados exclusivamente a partir do arquivo importado.</span></div><RouterLink class="btn outline" to="/app/upload">Nova análise</RouterLink></div>
    <div v-if="!a" class="panel empty-state"><b>Nenhuma planilha analisada</b><p>Importe um arquivo XLSX para gerar indicadores reais, identificar problemas de qualidade e consultar os registros.</p><RouterLink class="btn primary" to="/app/upload">Importar planilha →</RouterLink></div>
    <template v-else>
      <div class="data-state">● <b>Análise concluída</b>　 {{ dataStore.fileName }} <span>{{ a.totalRows }} linhas · {{ dataStore.headers.length }} colunas</span></div>
      <div class="kpis"><article><span>Total de clientes</span><b>{{ fmt(a.totalClients) }}</b><small>Identificadores únicos</small></article><article><span>Clientes ativos</span><b>{{ a.activeClients===null?'N/D':fmt(a.activeClients) }}</b><small>{{ a.activeClients===null?'Coluna de status não encontrada':'Conforme status da planilha' }}</small></article><article><span>Serviços encontrados</span><b>{{ fmt(a.services.length) }}</b><small>Categorias distintas</small></article><article><span>Qualidade estimada</span><b>{{ percent(a.quality) }}</b><small>{{ fmt(a.emptyCells) }} células vazias</small></article></div>
      <div class="dash-grid"><article class="panel"><p class="kicker">COMPOSIÇÃO</p><h2>Clientes por segmento</h2><div v-if="a.segments.length" class="bars"><p v-for="item in a.segments.slice(0,6)" :key="item.name">{{ item.name }}<i :style="`--w:${width(item.value,a.segments)}`"></i><b>{{ item.value }}</b></p></div><p v-else class="muted-copy">Coluna de segmento não encontrada.</p></article><article class="panel"><p class="kicker">PORTFÓLIO</p><h2>Serviços identificados</h2><div v-if="a.services.length" class="bars"><p v-for="item in a.services.slice(0,6)" :key="item.name">{{ item.name }}<i :style="`--w:${width(item.value,a.services)}`"></i><b>{{ item.value }}</b></p></div><p v-else class="muted-copy">Coluna de serviço não encontrada.</p></article><article class="panel"><p class="kicker">CARTEIRA</p><h2>Distribuição por consultor</h2><div v-if="a.consultants.length" class="bars"><p v-for="item in a.consultants.slice(0,6)" :key="item.name">{{ item.name }}<i :style="`--w:${width(item.value,a.consultants)}`"></i><b>{{ item.value }}</b></p></div><p v-else class="muted-copy">Coluna de consultor não encontrada.</p></article><article class="panel quality"><p class="kicker">QUALIDADE</p><h2>Diagnóstico <b>{{ percent(a.quality) }}</b></h2><progress :value="a.quality" max="100"></progress><p>✓　{{ fmt(a.totalRows-a.missingKey) }} registros com chave</p><p>!　{{ fmt(a.duplicates) }} possíveis duplicidades</p><p>×　{{ fmt(a.missingKey) }} registros sem chave</p></article></div>
      <aside class="insight">◇ <span><small>INSIGHT AUTOMÁTICO</small><b>{{ a.duplicates ? 'Há possíveis clientes duplicados para revisar' : 'Nenhuma duplicidade de chave foi detectada' }}</b><p>A análise é descritiva e depende da qualidade e dos nomes das colunas do arquivo.</p></span><RouterLink class="btn light" to="/app/relatorios">Ver registros</RouterLink></aside>
    </template>
  </section>
</template>
