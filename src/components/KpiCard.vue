<script setup>
// Cartão de indicador reaproveitado para média, clientes, contratos etc.
defineProps({
  titulo: { type: String, required: true },
  valor: { type: [String, Number], default: '-' },
  detalhe: { type: String, default: '' },
})
</script>

<template>
  <article class="panel kpi-card">
    <p class="kicker">{{ titulo }}</p>
    <strong>{{ valor }}</strong>
    <small v-if="detalhe">{{ detalhe }}</small>
  </article>
</template>

<style scoped>
.kpi-card { display: grid; gap: 4px; }
.kpi-card strong { font-size: 1.5rem; line-height: 1.2; }
.kpi-card small { color: var(--muted); }
</style>
