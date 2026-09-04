<script setup>
import { computed, ref } from 'vue'
const file=ref(null),progress=ref(0),status=ref('idle')
const size=computed(()=>file.value?`${(file.value.size/1048576).toFixed(2)} MB`:'')
const select=f=>{if(!f)return;file.value=f;status.value=f.name.toLowerCase().endsWith('.xlsx')?'ready':'invalid'}
const validate=()=>{status.value='processing';const t=setInterval(()=>{progress.value+=10;if(progress.value>=100){clearInterval(t);status.value='done'}},80)}
const reset=()=>{file.value=null;progress.value=0;status.value='idle'}
</script>

<template>
  <section class="page">
    <div class="page-head"><div><p class="kicker">NOVA IMPORTAÇÃO</p><h1>Importar dados</h1><span>Envie uma planilha comercial para iniciar um lote rastreável.</span></div><b>ETAPA 1 DE 4</b></div>
    <div class="steps"><b>① Arquivo</b><i></i><span>② Mapeamento</span><i></i><span>③ Qualidade</span><i></i><span>④ Publicação</span></div>
    <div class="upload-grid"><article class="panel upload-panel"><h2>⇧　Selecione o arquivo</h2><p>O arquivo será validado antes do processamento.</p><label class="dropzone" @dragover.prevent @drop.prevent="select($event.dataTransfer.files[0])"><input type="file" accept=".xlsx" @change="select($event.target.files[0])"><b>⇧</b><strong>Arraste sua planilha aqui</strong><span>ou <u>selecione um arquivo</u> no computador</span><small>Formato aceito: XLSX · Tamanho máximo: 20 MB</small></label><div v-if="file" class="file"><b>X</b><span><strong>{{ file.name }}</strong><small>{{ size }} · {{ status==='invalid'?'Formato inválido':status==='done'?'Validação concluída':'Pronto para validar' }}</small><progress v-if="status==='processing'" :value="progress" max="100"></progress></span><button @click="reset">×</button></div><p v-if="status==='invalid'" class="error">Envie um arquivo .xlsx para continuar.</p><footer><button class="btn outline" @click="reset">Cancelar</button><button class="btn primary" :disabled="status!=='ready'" @click="validate">{{ status==='processing'?`Validando ${progress}%`:status==='done'?'Arquivo validado':'Validar e continuar →' }}</button></footer></article><aside class="panel help"><p class="kicker">ANTES DE ENVIAR</p><h2>Prepare sua planilha</h2><ol><li><b>01</b><span><strong>Use o formato XLSX</strong>Arquivos com macros não são aceitos.</span></li><li><b>02</b><span><strong>Mantenha os cabeçalhos</strong>A primeira linha identifica cada coluna.</span></li><li><b>03</b><span><strong>Não altere os códigos</strong>Identificadores ajudam na deduplicação.</span></li></ol><aside><b>Privacidade e LGPD</b><p>Envie apenas dados autorizados. Todo processamento será registrado.</p></aside></aside></div>
    <article class="panel recent"><p class="kicker">HISTÓRICO RECENTE</p><h2>Últimas importações</h2><div class="table-wrap"><table><thead><tr><th>Arquivo</th><th>Protocolo</th><th>Data</th><th>Registros</th><th>Status</th></tr></thead><tbody><tr><td>carteira_clientes_agosto.xlsx</td><td>#IMP-0826-041</td><td>Hoje, 09:42</td><td>12.979</td><td><span>Publicado</span></td></tr><tr><td>base_comercial_julho.xlsx</td><td>#IMP-0726-038</td><td>01 ago, 16:18</td><td>12.486</td><td><em>Em revisão</em></td></tr></tbody></table></div></article>
  </section>
</template>
