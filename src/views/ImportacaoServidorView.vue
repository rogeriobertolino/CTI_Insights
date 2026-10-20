<script setup>
import { computed } from 'vue'
import { useUploadStore } from '../stores/uploadStore.js'
import { useAuthStore } from '../stores/auth.js'

const store = useUploadStore()
const auth = useAuthStore()
const temSegmento = computed(() => store.cabecalhos.includes('segmento'))
const podeEnviar = computed(() => !!auth.usuario && store.temDados && temSegmento.value && store.totalValidos > 0 && !store.carregando && !store.enviando && !store.analiseSalva)
async function importar() {
  if (podeEnviar.value) await store.enviarDados()
}
</script>

<template>
  <section class="page">
    <div class="page-head"><div><p class="kicker">ANÁLISES NO SERVIDOR</p><h1>Importação integrada</h1><span>Salve os registros validados no banco e gere a análise no servidor.</span></div></div>
    <article v-if="!store.temDados" class="panel">
      <h2>Valide uma planilha para começar</h2>
      <p class="validation-copy">Selecione o arquivo na página de validação e confira as ocorrências. Depois, continue para esta etapa. Os dados validados ficam disponíveis enquanto esta página do navegador estiver aberta.</p>
      <RouterLink class="btn primary" to="/app/upload">Selecionar e validar planilha →</RouterLink>
    </article>
    <article v-else class="panel">
      <h2>Confira o envio</h2>
      <dl><dt>Arquivo</dt><dd>{{ store.nomeArquivo }}</dd><dt>Registros encontrados</dt><dd>{{ store.totalRegistros }}</dd><dt>Registros válidos para enviar</dt><dd>{{ store.totalValidos }}</dd><dt>Registros com ocorrências, excluídos do envio</dt><dd>{{ store.totalComErro }}</dd></dl>
      <p class="validation-copy">Serão enviados os registros válidos de todos os segmentos. O servidor salvará os dados, gerará a análise e registrará a importação na auditoria.</p>
      <RouterLink v-if="!store.enviando" class="btn outline" to="/app/relatorios">Revisar relatório de validação</RouterLink>
      <RouterLink v-if="!store.enviando" class="btn outline" to="/app/upload">Trocar ou validar planilha</RouterLink>
      <p v-if="!temSegmento" class="validation-alert" role="alert">Inclua a coluna segmento na planilha e valide novamente para importar no servidor.</p>
      <p v-if="!store.totalValidos" class="validation-alert" role="alert">Nenhum registro válido para enviar. Corrija as ocorrências e valide novamente.</p>
      <p v-if="!auth.usuario" class="validation-copy">Para importar, <RouterLink to="/login">entre na sua conta</RouterLink>. Após entrar, retorne à Importação integrada pelo menu.</p>
      <p v-if="store.erroEnvio" class="validation-alert" role="alert">{{ store.erroEnvio }}</p>
      <p v-if="store.enviando" role="status">Importando registros e gerando a análise…</p>
      <div v-if="store.analiseSalva" role="status">
        <p>Importação concluída. Lote: {{ store.analiseSalva.batchId }}.</p>
        <RouterLink class="btn primary" to="/app/graficos">Abrir dashboard →</RouterLink>
        <RouterLink class="btn outline" to="/app/registros-servidor">Consultar registros persistidos</RouterLink>
      </div>
      <button v-else class="btn primary" :disabled="!podeEnviar" @click="importar">{{ store.enviando ? 'Importando…' : 'Confirmar importação no servidor' }}</button>
    </article>
  </section>
</template>
