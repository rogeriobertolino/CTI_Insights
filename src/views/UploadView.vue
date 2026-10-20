<script setup>
import { computed, ref } from 'vue'
import { useUploadStore } from '../stores/uploadStore.js'
import { baixarModeloAula as baixarModeloOficial } from '../utils/modeloPlanilha.js'
import PreviaPlanilha from '../components/PreviaPlanilha.vue'

const store = useUploadStore()
const arquivo = computed(() => store.arquivo)
const entrada = ref(null)
const sobreArea = ref(false)
const tamanho = computed(() => arquivo.value ? `${(arquivo.value.size / 1024).toFixed(1)} KB` : '')

function selecionar(file) {
  if (!file || (store.carregando || store.enviando)) return
  store.selecionarArquivo(file)
}
function soltar(event) {
  sobreArea.value = false
  selecionar(event.dataTransfer.files[0])
}
async function analisar() {
  if (!arquivo.value || (store.carregando || store.enviando)) return
  await store.processarPlanilha()
}
function limpar() {
  if (entrada.value) entrada.value.value = ''
  store.limpar()
}
</script>

<template>
  <section class="page validation-page">
    <div class="page-head"><div><p class="kicker">QUALIDADE DOS DADOS</p><h1>Validar planilha</h1><span>Encontre problemas antes de utilizar os dados dos seus clientes.</span></div><RouterLink v-if="store.validadoEm" class="btn outline" to="/app/relatorios">Ver último relatório →</RouterLink></div>
    <ol class="validation-steps" aria-label="Etapas da validação"><li class="current"><b>1</b> Escolher planilha</li><li><b>2</b> Validar dados</li><li><b>3</b> Consultar relatório</li></ol>
    <div class="upload-grid">
      <article class="panel upload-panel">
        <h2>Selecione sua planilha</h2><p class="validation-copy">A análise acontece neste navegador. O arquivo e os resultados ficam disponíveis somente durante esta sessão da página.</p>
        <label class="dropzone" :class="{ 'is-dragging': sobreArea }" @dragover.prevent="sobreArea = true" @dragleave.prevent="sobreArea = false" @drop.prevent="soltar"><input ref="entrada" type="file" accept=".xlsx,.xls,.csv" :disabled="(store.carregando || store.enviando)" aria-label="Selecionar planilha Excel" @change="selecionar($event.target.files[0])"><b aria-hidden="true">⇧</b><strong>Arraste uma planilha aqui</strong><span>ou <u>escolha no seu computador</u></span><small>.xlsx, .xls ou .csv · até 5 MB · até 10.000 registros</small></label>
        <div v-if="arquivo" class="file"><span class="excel-icon" aria-hidden="true">X</span><span><strong>{{ arquivo.name }}</strong><small>{{ tamanho }}</small></span><button class="btn outline" :disabled="(store.carregando || store.enviando)" aria-label="Remover arquivo" @click="limpar">Remover</button></div>
        <p v-if="store.erroArquivo" class="validation-alert" role="alert">{{ store.erroArquivo }}</p><p v-if="(store.carregando || store.enviando)" class="validation-copy" role="status">{{ store.enviando ? 'Enviando os registros válidos e salvando a análise…' : 'Lendo a planilha e verificando os registros…' }}</p>
        <footer><button class="btn outline" :disabled="(store.carregando || store.enviando) || (!arquivo && !store.validadoEm)" @click="limpar">Limpar</button><button class="btn primary" :disabled="!arquivo || (store.carregando || store.enviando)" @click="analisar">{{ (store.carregando || store.enviando) ? 'Validando…' : 'Processar e conferir prévia' }}</button></footer>
      </article>
      <aside class="panel validation-guide">
        <p class="kicker">PREPARE SEU ARQUIVO</p>
        <h2>Modelo oficial da planilha</h2>
        <p>Baixe o modelo e preencha os dados na primeira aba, mantendo os cabeçalhos.</p>
        <dl>
          <dt>Colunas do modelo</dt>
          <dd><code>codigo_cliente</code>, <code>cliente</code>, <code>consultor</code>, <code>segmento</code>, <code>nivel</code>, <code>faturamento_mensal</code>, <code>servico</code>, <code>valor_servico</code>, <code>status_contrato</code> e <code>data_inicio</code>.</dd>
          <dt>Regras principais</dt>
          <dd>Nível: A, B ou C.<br>Status do contrato: ATIVO, PENDENTE ou CANCELADO.<br>Valores: números não negativos, com até duas casas decimais.<br>Data: AAAA-MM-DD ou DD/MM/AAAA.</dd>
        </dl>
        <p>Use um código único para cada cliente. Evite espaços extras e nomes inteiramente em maiúsculas ou minúsculas.</p>
        <p>O arquivo contém dados de exemplo. Substitua-os pelos seus clientes antes de validar.</p>
        <button class="btn outline" :disabled="store.carregando || store.enviando" @click="baixarModeloOficial">↓ Baixar modelo oficial</button>
      </aside>
    </div>
    <PreviaPlanilha />
    <article v-if="store.temDados" class="panel">
      <h2>Próxima etapa: importar no servidor</h2>
      <p class="validation-copy">A validação local está concluída. Confira o relatório e continue para revisar os registros que serão salvos no banco.</p>
      <RouterLink class="btn primary" to="/app/importacao-servidor">Continuar para importação integrada →</RouterLink>
    </article>
    <RouterLink v-if="store.temDados" class="btn primary" to="/app/relatorios">Consultar relatório completo →</RouterLink>
    <article class="panel validation-rules"><h2>O que será verificado</h2><ul><li>Campos obrigatórios vazios</li><li>Espaços desnecessários</li><li>Códigos de cliente duplicados</li><li>E-mails inválidos</li><li>Códigos, status e níveis fora do padrão</li><li>Faturamento inválido, vazio ou negativo</li><li>Textos que precisam de padronização</li></ul><p class="validation-copy">Os valores originais são preservados. Linhas inteiramente vazias são ignoradas; os problemas indicam a linha real do Excel. Toda ocorrência torna o registro pendente de revisão.</p></article>

  </section>
</template>
