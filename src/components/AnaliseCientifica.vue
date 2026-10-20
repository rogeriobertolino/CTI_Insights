<script setup>
import { computed } from 'vue'
import CardIndicador from './CardIndicador.vue'
import { Bar, Line, Scatter, Doughnut } from 'vue-chartjs'
import { Chart as ChartJS, ArcElement, BarElement, LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Legend } from 'chart.js'
import { baixarRelatorio } from '../utils/exportarAnalise.js'
ChartJS.register(ArcElement, BarElement, LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Legend)
const props = defineProps({ analytics: { type: Object, required: true } })
const info = computed(() => props.analytics)
const charts = computed(() => info.value.charts || {})
const kpis = computed(() => info.value.indicators || {})
const stats = computed(() => info.value.statistics || {})
const tests = computed(() => info.value.statisticalTests || {})
const money = value => value == null ? 'N/D' : Number(value).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
const number = value => value == null ? 'N/D' : Number(value).toLocaleString('pt-BR', { maximumFractionDigits: 4 })
const line = computed(() => ({ labels: (charts.value.contractsByMonth || []).map(v => v.month), datasets: [{ label: 'Contratações', data: (charts.value.contractsByMonth || []).map(v => v.count), borderColor: '#006eb7', backgroundColor: '#006eb7' }] }))
const hist = computed(() => ({ labels: (charts.value.revenueHistogram || []).map(v => `${number(v.from)} a ${number(v.to)}`), datasets: [{ label: 'Registros', data: (charts.value.revenueHistogram || []).map(v => v.count), backgroundColor: '#006eb7' }] }))
const scatter = computed(() => ({ datasets: [{ label: 'Clientes', data: charts.value.revenueServices || [], backgroundColor: '#006eb7' }] }))
const levels = computed(() => ({ labels: (stats.value.byLevel || []).map(v => 'Nível ' + v.name), datasets: [{ data: (stats.value.byLevel || []).map(v => v.count), backgroundColor: ['#006eb7','#43a795','#e5a53a'] }] }))
const options = { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }
const scatterOptions = { ...options, scales: { x: { title: { display: true, text: 'Serviços distintos por cliente' }, ticks: { precision: 0 } }, y: { title: { display: true, text: 'Faturamento mensal (R$)' }, beginAtZero: true } } }
const groups = computed(() => (charts.value.revenueBySegment || []).filter(v => v.count))
const max = computed(() => Math.max(...groups.value.map(v => v.max), 1))
const pos = value => 150 + Number(value || 0) / max.value * 390
const measures = [['mean', 'Média'], ['median', 'Mediana'], ['modes', 'Moda(s)'], ['variance', 'Variância amostral (R$²)'], ['std', 'Desvio padrão amostral'], ['min', 'Mínimo'], ['max', 'Máximo'], ['range', 'Amplitude'], ['q1', 'Q1'], ['q2', 'Q2'], ['q3', 'Q3']]
const exports = computed(() => [
  ['dados_validos', info.value.validRecords || []], ['dados_invalidos', info.value.invalidRecords || []],
  ['estatistica_segmento', stats.value.bySegment || []], ['estatistica_nivel', stats.value.byLevel || []],
  ['indicadores_dashboard', [kpis.value]], ['insights', (info.value.insights || []).map(texto => ({ texto }))],
])
</script>
<template>
  <section aria-label="Ciência de dados">
    <article class="panel"><p class="kicker">CIÊNCIA DE DADOS</p><h2>Resultado do ETL</h2><p>{{ info.inputRows }} registros recebidos · {{ info.validRows }} válidos · {{ info.invalidRows }} inválidos.</p><p v-if="info.schema === 'clientes-legado'">Planilha de clientes: indicadores de contratos exigem as colunas do modelo oficial completo.</p><p v-if="info.schema === 'comercial-contratos-v1'">Os cálculos usam somente os registros válidos. Cada linha representa um cliente e seu contrato nesta importação.</p><div class="export-actions"><button v-for="[name, rows] in exports" :key="name" class="btn outline" :disabled="!rows.length" @click="baixarRelatorio(name + '.csv', rows)">↓ {{ name.replaceAll('_', ' ') }}</button></div></article>
    <div class="kpis grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
      <CardIndicador rotulo="Faturamento mensal total" :valor="money(kpis.totalRevenue)" />
      <CardIndicador rotulo="Ticket médio" :valor="money(kpis.averageTicket)" />
      <CardIndicador rotulo="Contratos ativos" :valor="number(kpis.activeContracts)" />
      <CardIndicador rotulo="Taxa de cancelamento" :valor="kpis.cancellationRate == null ? 'N/D' : number(kpis.cancellationRate) + '%'" />
    </div>
    <div class="dash-grid">
      <article class="panel"><h2>Proporção por nível A/B/C</h2><div v-if="stats.byLevel?.length" class="science-chart"><Doughnut :data="levels" :options="{ responsive: true, maintainAspectRatio: false }" /></div><p v-else>Sem classificação disponível.</p></article>
      <article class="panel"><h2>Contratações por mês</h2><div v-if="charts.contractsByMonth?.length" class="science-chart"><Line :data="line" :options="options" /></div><p v-else>Sem datas de contratação disponíveis.</p></article>
      <article class="panel"><h2>Histograma do faturamento</h2><div v-if="charts.revenueHistogram?.length" class="science-chart"><Bar :data="hist" :options="options" /></div><p v-else>Sem faturamento disponível.</p></article>
      <article class="panel"><h2>Boxplot por segmento</h2><svg v-if="groups.length" role="img" aria-label="Boxplot: caixa entre Q1 e Q3, mediana e limites de 1,5 vezes o intervalo interquartil" :viewBox="`0 0 570 ${groups.length * 52 + 20}`" style="width:100%"><g v-for="(g, i) in groups" :key="g.name" :transform="`translate(0,${i * 52 + 26})`"><text x="0" y="5" font-size="12">{{ g.name.slice(0,22) }}</text><line :x1="pos(g.lowerWhisker ?? g.min)" :x2="pos(g.upperWhisker ?? g.max)" y1="0" y2="0" stroke="#006eb7"/><rect :x="pos(g.q1)" y="-12" :width="Math.max(2, pos(g.q3) - pos(g.q1))" height="24" fill="#c9e6f7" stroke="#006eb7"/><line :x1="pos(g.median)" :x2="pos(g.median)" y1="-12" y2="12" stroke="#143c56"/><circle v-for="(v,j) in g.outliers || []" :key="j" :cx="pos(v)" cy="0" r="2" fill="#b33"/><text :x="pos(g.min)" y="27" font-size="10">{{ number(g.min) }}</text><text :x="pos(g.max)" y="27" font-size="10" text-anchor="end">{{ number(g.max) }}</text></g></svg><p v-else>Sem grupos com faturamento disponível.</p></article>
      <article class="panel"><h2>Faturamento × serviços</h2><div v-if="charts.revenueServices?.length" class="science-chart"><Scatter :data="scatter" :options="scatterOptions" /></div><p v-else>Sem dados de serviços e faturamento.</p><p class="muted-copy">Se todos os clientes tiverem um único serviço, não há variação suficiente para avaliar uma relação entre as variáveis.</p></article>
      <article class="panel"><h2>Estatística descritiva</h2><dl v-if="stats.overall?.count"><template v-for="[key, label] in measures" :key="key"><dt>{{ label }}</dt><dd>{{ Array.isArray(stats.overall[key]) ? stats.overall[key].map(number).join(', ') : number(stats.overall[key]) }}</dd></template></dl><p v-else>Sem amostra válida.</p></article>
      <article class="panel"><h2>Testes estatísticos</h2><div v-for="[key, label] in [['shapiroWilk', 'Shapiro-Wilk: normalidade'], ['chiSquare', 'Qui-quadrado: segmento × nível']]" :key="key"><h3>{{ label }}</h3><p v-if="tests[key]?.available">Estatística: {{ number(tests[key].statistic) }} · p-valor: {{ tests[key].pValue?.toPrecision(4) }} · α = 0,05</p><p>{{ tests[key]?.interpretation || tests[key]?.reason || 'Não calculado neste lote.' }}</p></div></article>
    </div>
    <article class="panel"><h2>Insights da importação</h2><ul><li v-for="text in info.insights || []" :key="text">{{ text }}</li></ul><p v-if="!info.insights?.length">Dados insuficientes para gerar insights.</p></article>
  </section>
</template>
<style scoped>.science-chart{height:280px}.export-actions{display:flex;flex-wrap:wrap;gap:8px;margin-top:16px}dd{margin-bottom:8px}svg{overflow:visible}</style>
