<script setup>
import { computed, ref } from 'vue'
const q=ref('')
const clients=[['Nexa Tecnologia Ltda.','CLI-001248','Tecnologia','A','Ana Martins',4,'Ativo'],['Indústria Horizonte S.A.','CLI-001247','Indústria','A','Carlos Lima',3,'Ativo'],['Mercado Via Sul Ltda.','CLI-001246','Varejo','B','Ana Martins',2,'Ativo'],['Grupo Método Serviços','CLI-001245','Serviços','B','Paulo Reis',1,'Em revisão'],['Aurora Sistemas S.A.','CLI-001244','Tecnologia','C','Carlos Lima',2,'Inativo']]
const filtered=computed(()=>clients.filter(c=>c.join(' ').toLowerCase().includes(q.value.toLowerCase())))
const exportCsv=()=>{const blob=new Blob([['Cliente;Código;Segmento;Nível;Consultor;Serviços;Status',...filtered.value.map(x=>x.join(';'))].join('\n')],{type:'text/csv'});const a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download='carteira.csv';a.click()}
</script>

<template>
  <section class="page">
    <div class="page-head"><div><p class="kicker">BASE PUBLICADA</p><h1>Carteira de clientes</h1><span>Consulte, filtre e exporte os dados comerciais autorizados.</span></div><button class="btn primary" @click="exportCsv">↓ Exportar CSV</button></div>
    <div class="report-summary"><p>Total encontrado<b>{{ filtered.length }}</b></p><p>Clientes ativos<b>{{ filtered.filter(x=>x[6]==='Ativo').length }}</b></p><aside>● Dados: versão 2026.09.03<br><small>Atualizado hoje, 14:32</small></aside></div>
    <article class="panel report"><div class="filters"><input v-model="q" type="search" placeholder="⌕  Buscar por cliente ou código"><select><option>Todos os segmentos</option><option>Tecnologia</option><option>Indústria</option></select><select><option>Todos os status</option><option>Ativo</option><option>Inativo</option></select><button class="btn outline" @click="q=''">Limpar</button></div><div class="table-wrap"><table><thead><tr><th>Cliente</th><th>Segmento</th><th>Nível</th><th>Consultor</th><th>Serviços</th><th>Status</th></tr></thead><tbody><tr v-for="c in filtered" :key="c[1]"><td><b>{{ c[0] }}</b><small>{{ c[1] }}</small></td><td>{{ c[2] }}</td><td><i>{{ c[3] }}</i></td><td>{{ c[4] }}</td><td>{{ c[5] }}</td><td><span :class="c[6]==='Ativo'?'ok':'muted'">● {{ c[6] }}</span></td></tr></tbody></table></div></article>
    <aside class="notice">ⓘ <span><b>Indicadores financeiros ainda não disponíveis</b><p>ROI, margem, receita CTI e churn permanecem desabilitados até a validação das fontes e regras.</p></span></aside>
  </section>
</template>
