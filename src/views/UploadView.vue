<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearData, dataStore, uploadWorkbook } from '../dataStore.js'

const file=ref(null),status=ref('idle'),error=ref(''),router=useRouter()
const size=computed(()=>file.value?`${(file.value.size/1048576).toFixed(2)} MB`:'')
const select=value=>{if(!value)return;file.value=value;error.value='';status.value=value.name.toLowerCase().endsWith('.xlsx')&&value.size<=20*1048576?'ready':'invalid'}
const analyze=async()=>{status.value='processing';try{await uploadWorkbook(file.value);status.value='done'}catch(e){error.value=e.message;status.value='error'}}
const reset=()=>{file.value=null;status.value='idle';error.value='';clearData()}
</script>

<template>
  <section class="page">
    <div class="page-head"><div><p class="kicker">NOVA IMPORTAÇÃO</p><h1>Importar dados</h1><span>Envie uma planilha para o pipeline integrado de análise.</span></div><b>EXCEL → JAVA → PYTHON → POSTGRESQL</b></div>
    <div class="steps"><b>① Upload Vue</b><i></i><span>② API Java</span><i></i><span>③ Análise Python</span><i></i><span>④ Persistência</span></div>
    <div class="upload-grid">
      <article class="panel upload-panel"><h2>⇧　Selecione o arquivo XLSX</h2><p>O Spring Boot recebe o arquivo, aciona o Python e persiste o resultado no PostgreSQL.</p><label class="dropzone" @dragover.prevent @drop.prevent="select($event.dataTransfer.files[0])"><input type="file" accept=".xlsx" @change="select($event.target.files[0])"><b>⇧</b><strong>Arraste sua planilha aqui</strong><span>ou <u>selecione um arquivo</u></span><small>XLSX · até 20 MB</small></label><div v-if="file" class="file"><b>X</b><span><strong>{{ file.name }}</strong><small>{{ size }} · {{ status==='done'?'Análise persistida':status==='processing'?'Processando no servidor...':'Pronto para enviar' }}</small><progress v-if="status==='processing'"></progress></span><button @click="reset">×</button></div><p v-if="status==='invalid'" class="error">Envie um XLSX de até 20 MB.</p><p v-if="error" class="error">{{ error }}</p><footer><button class="btn outline" @click="reset">Limpar</button><button v-if="status!=='done'" class="btn primary" :disabled="status!=='ready'" @click="analyze">{{ status==='processing'?'Analisando...':'Enviar e analisar →' }}</button><button v-else class="btn primary" @click="router.push('/app/graficos')">Ver dashboard →</button></footer></article>
      <aside class="panel help"><p class="kicker">PIPELINE</p><h2>Tratamento governado</h2><ol><li><b>01</b><span><strong>Spring Boot</strong>Valida o arquivo e coordena o lote.</span></li><li><b>02</b><span><strong>Python</strong>Padroniza e calcula métricas.</span></li><li><b>03</b><span><strong>PostgreSQL</strong>Persiste os resultados consolidados.</span></li></ol><aside><b>Privacidade e LGPD</b><p>O arquivo temporário é excluído após o processamento.</p></aside></aside>
    </div>
    <article v-if="dataStore.analysis" class="panel recent"><p class="kicker">RESULTADO PERSISTIDO</p><h2>{{ dataStore.analysis.fileName }}</h2><div class="report-summary"><p>Linhas analisadas<b>{{ dataStore.analysis.totalRows }}</b></p><p>Colunas<b>{{ dataStore.analysis.headers?.length||0 }}</b></p><p>Qualidade<b>{{ dataStore.analysis.quality.toFixed(1) }}%</b></p><aside>● Lote {{ dataStore.analysis.batchId }}</aside></div></article>
  </section>
</template>
