<script setup>
// Dashboard do ETL (Aula 12): GET /api/dashboard?loteId=N devolve o JSON gravado pelo Python.
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseChart from '../components/BaseChart.vue'
import KpiCard from '../components/KpiCard.vue'
import { listarLotes, obterDashboard } from '../services/uploads.js'

const route = useRoute()
const router = useRouter()
const dash = ref(null)
const lotes = ref([])
const carregando = ref(false)
const erro = ref('')
let timer = null
let versao = 0

const d = computed(() => dash.value?.estatisticas)
const resumo = computed(() => d.value?.resumo ?? {})
const seg = computed(() => d.value?.porSegmento ?? [])
const nivel = computed(() => d.value?.porNivel ?? [])
const mes = computed(() => d.value?.porMes ?? [])
const servico = computed(() => d.value?.porServico ?? [])
const consultor = computed(() => d.value?.porConsultor ?? [])
const loteId = computed(() => route.query.loteId ? String(route.query.loteId) : '')

const moeda = (v) => v == null ? '-' : Number(v).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
const numero = (v, casas = 4) => v == null ? '-' : Number(v).toLocaleString('pt-BR', { maximumFractionDigits: casas })
const pValor = (p) => p == null ? '-' : p < 0.0001 ? '< 0,0001' : numero(p)

async function carregar() {
  const consulta = ++versao
  clearTimeout(timer)
  carregando.value = true
  erro.value = ''
  try {
    const [painel, lista] = await Promise.all([obterDashboard(loteId.value || undefined), listarLotes()])
    if (consulta !== versao) return
    dash.value = painel
    lotes.value = lista
  } catch (e) {
    if (consulta !== versao) return
    dash.value = null
    erro.value = e.message
    const lista = await listarLotes().catch(() => [])
    if (consulta !== versao) return
    lotes.value = lista
  } finally {
    if (consulta === versao) {
      carregando.value = false
      const selecionado = lotes.value.find(l => String(l.id) === loteId.value)
      if (selecionado && ['PENDENTE', 'PROCESSANDO', 'AGUARDANDO_ETL'].includes(selecionado.status)) timer = setTimeout(carregar, 5000)
    }
  }
}

function escolher(evento) {
  router.replace({ query: evento.target.value ? { loteId: evento.target.value } : {} })
}

onMounted(carregar)
watch(loteId, carregar)
onBeforeUnmount(() => { ++versao; clearTimeout(timer) })
</script>

<template>
  <section class="page">
    <div class="page-head flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p class="kicker">ETL EM PYTHON · AULA 12</p>
        <h1>Dashboard do lote</h1>
        <span v-if="dash">Lote {{ dash.lote.id }} · {{ dash.lote.arquivoNome }} · {{ new Date(dash.lote.processadoEm || dash.lote.criadoEm).toLocaleString('pt-BR') }}</span>
      </div>
      <label class="flex flex-col gap-1 text-sm">Lote
        <select class="rounded-lg border border-slate-300 p-2" :value="loteId" @change="escolher">
          <option value="">Último processado</option>
          <option v-for="l in lotes" :key="l.id" :value="String(l.id)">#{{ l.id }} · {{ l.arquivoNome }} · {{ l.status }}</option>
        </select>
      </label>
    </div>

    <p v-if="carregando" role="status">Carregando…</p>
    <article v-else-if="erro" class="panel" role="alert">
      <h2>Não foi possível abrir o dashboard</h2>
      <p>{{ erro }}</p>
      <RouterLink class="btn primary" to="/app/upload">Enviar uma planilha</RouterLink>
    </article>

    <template v-else-if="d">
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard titulo="MÉDIA DE FATURAMENTO" :valor="moeda(resumo.media)" :detalhe="`Mediana ${moeda(resumo.mediana)}`" />
        <KpiCard titulo="CLIENTES" :valor="resumo.clientes" :detalhe="`${resumo.linhasRecebidas} linhas recebidas`" />
        <KpiCard titulo="CONTRATOS" :valor="resumo.contratos" detalhe="1 por serviço contratado" />
        <KpiCard titulo="FATURAMENTO TOTAL" :valor="moeda(resumo.total)" :detalhe="`Desvio padrão ${moeda(resumo.desvioPadrao)}`" />
      </div>

      <article class="panel mt-4">
        <h2>Insights</h2>
        <p v-if="mes.some(x => x.clientes == null)" class="text-sm">Este lote usa a análise anterior. Envie novamente a planilha para obter a série mensal de clientes.</p>
        <ul class="list-disc pl-5"><li v-for="frase in d.insights" :key="frase">{{ frase }}</li></ul>
      </article>

      <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
        <BaseChart tipo="bar" titulo="Clientes por segmento" :rotulos="seg.map(x => x.segmento)" :valores="seg.map(x => x.clientes)" />
        <BaseChart tipo="doughnut" titulo="Carteira por nível A/B/C" :rotulos="nivel.map(x => 'Nível ' + x.nivel)" :valores="nivel.map(x => x.clientes)" />
        <BaseChart tipo="line" titulo="Clientes contratantes por mês" :rotulos="mes.map(x => x.mes)" :valores="mes.map(x => x.clientes ?? null)" />
        <BaseChart tipo="bar" horizontal titulo="Serviços mais contratados" :rotulos="servico.map(x => x.servico)" :valores="servico.map(x => x.contratos)" />
        <BaseChart tipo="bar" titulo="Faturamento médio por segmento (R$)" :rotulos="seg.map(x => x.segmento)" :valores="seg.map(x => x.faturamentoMedio)" />
        <BaseChart tipo="bar" horizontal titulo="Faturamento por consultor (R$)" :rotulos="consultor.map(x => x.consultor)" :valores="consultor.map(x => x.faturamentoTotal)" />
      </div>

      <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
        <article class="panel">
          <h2>5 maiores clientes</h2>
          <div class="table-wrap"><table>
            <thead><tr><th>Código</th><th>Cliente</th><th>Segmento</th><th>Nível</th><th>Faturamento</th></tr></thead>
            <tbody><tr v-for="c in d.topClientes" :key="c.codigoCliente"><td>{{ c.codigoCliente }}</td><td>{{ c.nomeCliente }}</td><td>{{ c.segmento }}</td><td>{{ c.nivel }}</td><td>{{ moeda(c.faturamentoAnual) }}</td></tr></tbody>
          </table></div>
        </article>
        <article class="panel">
          <h2>Estatística</h2>
          <dl class="grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
            <dt>Q1 / Q3</dt><dd>{{ moeda(resumo.q1) }} / {{ moeda(resumo.q3) }}</dd>
            <dt>Mínimo / máximo</dt><dd>{{ moeda(resumo.minimo) }} / {{ moeda(resumo.maximo) }}</dd>
            <dt>Moda</dt><dd>{{ d.moda.segmento }} · nível {{ d.moda.nivel }} · {{ d.moda.servico }}</dd>
            <dt>Correlação (serviços × faturamento)</dt><dd>{{ d.correlacao.disponivel ? `r = ${numero(d.correlacao.coeficiente)} · p ${pValor(d.correlacao.pValor)}` : d.correlacao.motivo }}</dd>
          </dl>
          <h3 class="mt-3">Testes de hipótese</h3>
          <p v-for="[chave, nome] in [['shapiroWilk', 'Shapiro-Wilk'], ['quiQuadrado', 'Qui-quadrado']]" :key="chave" class="text-sm">
            <b>{{ nome }}:</b>
            <template v-if="d.testes[chave].disponivel"> estatística {{ numero(d.testes[chave].estatistica) }}, p {{ pValor(d.testes[chave].pValor) }}. {{ d.testes[chave].interpretacao }}</template>
            <template v-else> {{ d.testes[chave].motivo }}</template>
          </p>
          <p class="mt-3 text-sm">Qualidade: {{ resumo.linhasInvalidasNoBackend }} linha(s) barradas antes do Python, {{ resumo.linhasRejeitadasNoPython }} recusadas na limpeza e {{ resumo.correcoesFeitas }} correções automáticas de texto.</p>
        </article>
      </div>

      <article v-if="d.rejeitadas?.length" class="panel mt-4">
        <h2>Linhas recusadas pelo Python</h2>
        <ul class="list-disc pl-5 text-sm"><li v-for="r in d.rejeitadas" :key="r.linha">Linha {{ r.linha }} ({{ r.codigoCliente || 'sem código' }}): {{ r.motivo }}</li></ul>
      </article>
    </template>
  </section>
</template>
