<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { analyzeWorkbook, clearData, dataStore } from '../dataStore.js'
const file=ref(null),progress=ref(0),status=ref('idle')
const error=ref('')
const router=useRouter()
const size=computed(()=>file.value?`${(file.value.size/1048576).toFixed(2)} MB`:'')
const select=f=>{if(!f)return;file.value=f;error.value='';status.value=f.name.toLowerCase().endsWith('.xlsx')&&f.size<=20*1048576?'ready':'invalid'}
const validate=async()=>{status.value='processing';progress.value=25;try{await analyzeWorkbook(file.value);progress.value=100;status.value='done'}catch(e){error.value=e.message||'Não foi possível analisar a planilha.';status.value='error'}}
const reset=()=>{file.value=null;progress.value=0;status.value='idle';error.value='';clearData()}
</script>

<template>
  <section class="page">
    <div class="page-head"><div><p class="kicker">NOVA IMPORTAÇÃO</p><h1>Importar dados</h1><span>Envie uma planilha comercial para iniciar um lote rastreável.</span></div><b>ETAPA 1 DE 4</b></div>
    <div class="steps"><b>① Arquivo</b><i></i><span>② Leitura</span><i></i><span>③ Qualidade</span><i></i><span>④ Análise</span></div>
    <div class="upload-grid"><article class="panel upload-panel"><h2>⇧　Selecione o arquivo</h2><p>A análise ocorre localmente no navegador; nenhum dado é enviado a um servidor.</p><label class="dropzone" @dragover.prevent @drop.prevent="select($event.dataTransfer.files[0])"><input type="file" accept=".xlsx" @change="select($event.target.files[0])"><b>⇧</b><strong>Arraste sua planilha aqui</strong><span>ou <u>selecione um arquivo</u> no computador</span><small>Formato aceito: XLSX · Tamanho máximo: 20 MB</small></label><div v-if="file" class="file"><b>X</b><span><strong>{{ file.name }}</strong><small>{{ size }} · {{ status==='invalid'?'Arquivo inválido':status==='done'?'Análise concluída':'Pronto para analisar' }}</small><progress v-if="status==='processing'" :value="progress" max="100"></progress></span><button @click="reset">×</button></div><p v-if="status==='invalid'" class="error">Envie um arquivo .xlsx de até 20 MB.</p><p v-if="error" class="error">{{ error }}</p><footer><button class="btn outline" @click="reset">Limpar</button><button v-if="status!=='done'" class="btn primary" :disabled="status!=='ready'" @click="validate">{{ status==='processing'?'Analisando...':'Analisar planilha →' }}</button><button v-else class="btn primary" @click="router.push('/app/graficos')">Ver análise →</button></footer></article><aside class="panel help"><p class="kicker">ANÁLISE AUTOMÁTICA</p><h2>O que será verificado</h2><ol><li><b>01</b><span><strong>Estrutura</strong>Abas, colunas e quantidade de registros.</span></li><li><b>02</b><span><strong>Qualidade</strong>Campos vazios, chaves ausentes e duplicidades.</span></li><li><b>03</b><span><strong>Composição</strong>Segmentos, status, serviços, níveis e consultores.</span></li></ol><aside><b>Privacidade e LGPD</b><p>Os dados ficam apenas na memória desta sessão do navegador.</p></aside></aside></div>
    <article v-if="dataStore.analysis" class="panel recent"><p class="kicker">RESULTADO DO ARQUIVO</p><h2>{{ dataStore.fileName }}</h2><div class="report-summary"><p>Linhas analisadas<b>{{ dataStore.analysis.totalRows }}</b></p><p>Colunas<b>{{ dataStore.headers.length }}</b></p><p>Qualidade estimada<b>{{ dataStore.analysis.quality.toFixed(1) }}%</b></p></div></article>
  </section>
</template>
