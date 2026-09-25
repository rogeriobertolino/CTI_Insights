<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { usePlanilhaStore } from '../stores/planilha.js'
import { criarArquivoExemplo, baixarModelo } from '../utils/modeloPlanilha.js'

const store = usePlanilhaStore()
const router = useRouter()
const arquivo = ref(null)
const entrada = ref(null)
const sobreArea = ref(false)
const tamanho = computed(() => arquivo.value ? `${(arquivo.value.size / 1024).toFixed(1)} KB` : '')

function selecionar(file) {
  if (!file || store.carregando) return
  arquivo.value = file
  store.limpar()
}
function soltar(event) {
  sobreArea.value = false
  selecionar(event.dataTransfer.files[0])
}
async function analisar() {
  if (!arquivo.value || store.carregando) return
  if (await store.importarArquivo(arquivo.value)) router.push('/app/relatorios')
}
async function demonstrar() {
  selecionar(criarArquivoExemplo(true))
  await analisar()
}
function limpar() {
  arquivo.value = null
  if (entrada.value) entrada.value.value = ''
  store.limpar()
}
</script>

<template>
  <section class="page validation-page">
    <div class="page-head"><div><p class="kicker">QUALIDADE DOS DADOS</p><h1>Validar planilha</h1><span>Encontre problemas antes de utilizar os dados dos seus clientes.</span></div><RouterLink v-if="store.validadoEm" class="btn outline" to="/app/relatorios">Ver último relatório →</RouterLink></div>
    <ol class="validation-steps" aria-label="Etapas da validação"><li class="current"><b>1</b> Escolher Excel</li><li><b>2</b> Validar dados</li><li><b>3</b> Consultar relatório</li></ol>
    <div class="upload-grid">
      <article class="panel upload-panel">
        <h2>Selecione sua planilha</h2><p class="validation-copy">A análise acontece neste navegador. O arquivo e os resultados ficam disponíveis somente durante esta sessão da página.</p>
        <label class="dropzone" :class="{ 'is-dragging': sobreArea }" @dragover.prevent="sobreArea = true" @dragleave.prevent="sobreArea = false" @drop.prevent="soltar"><input ref="entrada" type="file" accept=".xlsx" :disabled="store.carregando" aria-label="Selecionar planilha Excel" @change="selecionar($event.target.files[0])"><b aria-hidden="true">⇧</b><strong>Arraste um arquivo Excel aqui</strong><span>ou <u>escolha no seu computador</u></span><small>.xlsx · até 5 MB · até 10.000 registros</small></label>
        <div v-if="arquivo" class="file"><span class="excel-icon" aria-hidden="true">X</span><span><strong>{{ arquivo.name }}</strong><small>{{ tamanho }}</small></span><button class="btn outline" :disabled="store.carregando" aria-label="Remover arquivo" @click="limpar">Remover</button></div>
        <p v-if="store.erroArquivo" class="validation-alert" role="alert">{{ store.erroArquivo }}</p><p v-if="store.carregando" class="validation-copy" role="status">Lendo a planilha e verificando os registros…</p>
        <footer><button class="btn outline" :disabled="store.carregando || (!arquivo && !store.validadoEm)" @click="limpar">Limpar</button><button class="btn primary" :disabled="!arquivo || store.carregando" @click="analisar">{{ store.carregando ? 'Validando…' : 'Validar e ver relatório →' }}</button></footer>
      </article>
      <aside class="panel validation-guide"><p class="kicker">PREPARE SEU ARQUIVO</p><h2>Modelo da planilha</h2><p>Use a primeira aba, com os cabeçalhos na primeira linha preenchida.</p><dl><dt>Obrigatórios</dt><dd><code>codigo</code>, <code>nome</code> e <code>email</code></dd><dt>Opcionais</dt><dd><code>status</code>: Ativo, Inativo ou Pendente.<br><code>nivel</code>: A, B ou C.</dd></dl><p>O código deve ser único. Use nomes com maiúsculas e minúsculas, e e-mails em minúsculas. Evite espaços nas extremidades e espaços repetidos.</p><button class="btn outline" :disabled="store.carregando" @click="baixarModelo">↓ Baixar modelo Excel</button><button class="btn text-button" :disabled="store.carregando" @click="demonstrar">Testar exemplo com problemas →</button></aside>
    </div>
    <article class="panel validation-rules"><h2>O que será verificado</h2><ul><li>Campos obrigatórios vazios</li><li>Espaços desnecessários</li><li>Códigos de cliente duplicados</li><li>E-mails inválidos</li><li>Códigos, status e níveis fora do padrão</li><li>Textos que precisam de padronização</li></ul><p class="validation-copy">Os valores originais são preservados. Linhas inteiramente vazias são ignoradas; os problemas indicam a linha real do Excel. Toda ocorrência torna o registro pendente de revisão.</p></article>
    <p class="validation-copy">Precisa armazenar uma análise no servidor? <RouterLink to="/app/importacao-servidor">Acessar importação integrada</RouterLink>.</p>
  </section>
</template>
