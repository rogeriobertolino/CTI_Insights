<script setup>
// Desenha um gráfico Chart.js no <canvas>. tipo: bar, line ou doughnut.
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Chart from 'chart.js/auto'

const props = defineProps({
  tipo: { type: String, default: 'bar' },
  titulo: { type: String, required: true },
  rotulos: { type: Array, default: () => [] },
  valores: { type: Array, default: () => [] },
  horizontal: { type: Boolean, default: false },
})

const CORES = ['#006eb7', '#e30613', '#2f9e44', '#f08c00', '#7048e8', '#1098ad', '#868e96']
const canvas = ref(null)
let grafico = null

function desenhar() {
  grafico?.destroy()
  const pizza = props.tipo === 'doughnut'
  grafico = new Chart(canvas.value, {
    type: props.tipo,
    data: {
      labels: props.rotulos,
      datasets: [{
        label: props.titulo,
        data: props.valores,
        backgroundColor: pizza ? CORES : props.tipo === 'line' ? 'rgba(0,110,183,.15)' : '#006eb7',
        borderColor: pizza ? '#fff' : '#006eb7',
        fill: props.tipo === 'line',
        tension: 0.25,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      indexAxis: props.horizontal ? 'y' : 'x',
      plugins: { legend: { display: pizza, position: 'bottom' } },
      scales: pizza ? {} : { y: { beginAtZero: true, ticks: { precision: 0 } } },
    },
  })
}

onMounted(desenhar)
watch(() => [props.tipo, props.rotulos, props.valores], desenhar, { deep: true })
onBeforeUnmount(() => grafico?.destroy())
</script>

<template>
  <article class="panel">
    <h2>{{ titulo }}</h2>
    <div class="base-chart"><canvas ref="canvas" role="img" :aria-label="titulo"></canvas></div>
  </article>
</template>

<style scoped>
.base-chart { position: relative; height: 280px; }
</style>
