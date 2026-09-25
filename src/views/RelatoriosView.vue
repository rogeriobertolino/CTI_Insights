<script setup>
import { computed, ref, watch } from 'vue'
import { usePlanilhaStore, TIPOS_ERRO } from '../stores/planilha.js'

const store = usePlanilhaStore()
const busca = ref('')
const tipo = ref('')
const pagina = ref(1)
const paginaRegistros = ref(1)
const porPagina = 25
const ocorrencias = computed(() => store.erros.filter(erro => {
  const termo = busca.value.trim().toLocaleLowerCase('pt-BR')
  return (!tipo.value || erro.tipo === tipo.value) && (!termo || [erro.linha, erro.campo, erro.valor, erro.descricao, erro.sugestao].join(' ').toLocaleLowerCase('pt-BR').includes(termo))
}))
const paginas = computed(() => Math.max(1, Math.ceil(ocorrencias.value.length / porPagina)))
const errosVisiveis = computed(() => ocorrencias.value.slice((pagina.value - 1) * porPagina, pagina.value * porPagina))
const paginasRegistros = computed(() => Math.max(1, Math.ceil(store.totalRegistros / porPagina)))
const registrosVisiveis = computed(() => store.registros.slice((paginaRegistros.value - 1) * porPagina, paginaRegistros.value * porPagina))
const linhasComErro = computed(() => new Set(store.erros.map(erro => erro.linha)))
const dataValidacao = computed(() => store.validadoEm ? new Date(store.validadoEm).toLocaleString('pt-BR') : '')
watch([busca, tipo], () => { pagina.value = 1 })
watch(() => store.validadoEm, () => { pagina.value = 1; paginaRegistros.value = 1 })
</script>

<template>
  <section class="page validation-page">
    <div class="page-head"><div><p class="kicker">RESULTADO DA VALIDAÇÃO</p><h1>Relatório da planilha</h1><span>Consulte os registros e os problemas encontrados em cada campo.</span></div><RouterLink class="btn primary" to="/app/upload">Validar outra planilha</RouterLink></div>
    <article v-if="!store.validadoEm" class="panel empty-state"><b>Nenhuma validação nesta sessão</b><p>Escolha uma planilha Excel para ver o relatório. Ao atualizar ou fechar a página, os dados temporários são apagados.</p><RouterLink class="btn primary" to="/app/upload">Selecionar planilha →</RouterLink></article>
    <template v-else>
      <div class="validation-file-summary"><div><span>Arquivo analisado</span><strong>{{ store.nomeArquivo }}</strong></div><div><span>Aba</span><strong>{{ store.nomeAba }}</strong></div><div><span>Validado em</span><strong>{{ dataValidacao }}</strong></div></div>
      <div class="kpis validation-kpis"><article><span>Total de registros</span><b>{{ store.totalRegistros }}</b><small>Linhas com dados</small></article><article><span>Registros válidos</span><b>{{ store.totalValidos }}</b><small>Sem ocorrências</small></article><article><span>Registros com erro</span><b>{{ store.totalComErro }}</b><small>Contados uma única vez por linha</small></article><article><span>Total de ocorrências</span><b>{{ store.totalErros }}</b><small>Uma linha pode ter vários problemas</small></article></div>
      <article class="panel"><div class="validation-section-head"><h2>Validações realizadas</h2><button class="btn outline" @click="store.validarDados()">Validar novamente</button></div><div class="validation-counts"><div v-for="item in store.errosPorTipo" :key="item.tipo"><span>{{ item.rotulo }}</span><b :class="{ 'has-issues': item.quantidade > 0 }">{{ item.quantidade }}</b></div></div><p class="validation-copy">As quantidades por tipo representam ocorrências. Um registro é válido somente quando não tem nenhuma ocorrência.</p></article>
      <article class="panel">
        <div class="validation-section-head"><h2>Detalhamento dos problemas</h2><span>{{ ocorrencias.length }} ocorrência(s)</span></div>
        <div class="filters validation-filters"><label><span>Buscar linha, campo ou descrição</span><input v-model="busca" type="search" placeholder="Ex.: email ou 3"></label><label><span>Tipo de erro</span><select v-model="tipo"><option value="">Todos os tipos</option><option v-for="(rotulo, chave) in TIPOS_ERRO" :key="chave" :value="chave">{{ rotulo }}</option></select></label></div>
        <div v-if="!store.totalErros" class="validation-success" role="status">✓ Todos os {{ store.totalRegistros }} registros passaram pelas regras de validação.</div>
        <template v-else><div class="table-wrap"><table class="validation-table"><caption class="sr-only">Ocorrências com linha do Excel, campo, valor original e descrição</caption><thead><tr><th scope="col">Linha Excel</th><th scope="col">Campo</th><th scope="col">Valor original</th><th scope="col">Problema encontrado</th><th scope="col">Sugestão</th></tr></thead><tbody><tr v-for="(erro, index) in errosVisiveis" :key="`${pagina}-${index}`"><td>{{ erro.linha }}</td><td>{{ erro.campo }}</td><td class="original-value">{{ erro.valor === '' || erro.valor == null ? '(vazio)' : erro.valor }}</td><td><strong>{{ TIPOS_ERRO[erro.tipo] }}</strong><span>{{ erro.descricao }}</span></td><td>{{ erro.sugestao || 'Revisar na planilha' }}</td></tr><tr v-if="!errosVisiveis.length"><td colspan="5">Nenhum problema corresponde aos filtros.</td></tr></tbody></table></div><nav class="validation-pagination" aria-label="Páginas dos problemas"><button class="btn outline" :disabled="pagina <= 1" @click="pagina--">Anterior</button><span>Página {{ pagina }} de {{ paginas }}</span><button class="btn outline" :disabled="pagina >= paginas" @click="pagina++">Próxima</button></nav></template>
      </article>
      <article class="panel"><div class="validation-section-head"><h2>Registros originais</h2><span>{{ store.totalValidos }} de {{ store.totalRegistros }} válidos</span></div><p class="validation-copy">Os dados abaixo são os valores importados, sem correções automáticas.</p><div class="table-wrap"><table class="validation-table"><caption class="sr-only">Registros originais e resultado da validação</caption><thead><tr><th scope="col">Linha Excel</th><th scope="col">Validação</th><th v-for="campo in store.cabecalhos" :key="campo" scope="col">{{ campo }}</th></tr></thead><tbody><tr v-for="registro in registrosVisiveis" :key="registro.linha"><td>{{ registro.linha }}</td><td><span class="validation-badge" :class="{ invalid: linhasComErro.has(registro.linha) }">{{ linhasComErro.has(registro.linha) ? 'Com erro' : 'Válido' }}</span></td><td v-for="campo in store.cabecalhos" :key="campo" class="original-value">{{ registro.dados[campo] === '' ? '—' : registro.dados[campo] }}</td></tr></tbody></table></div><nav class="validation-pagination" aria-label="Páginas dos registros"><button class="btn outline" :disabled="paginaRegistros <= 1" @click="paginaRegistros--">Anterior</button><span>Página {{ paginaRegistros }} de {{ paginasRegistros }}</span><button class="btn outline" :disabled="paginaRegistros >= paginasRegistros" @click="paginaRegistros++">Próxima</button></nav></article>
      <p class="validation-copy">Corrija os problemas no arquivo original e importe-o novamente. Os resultados são temporários e serão apagados ao atualizar a página.</p>
    </template>
  </section>
</template>
