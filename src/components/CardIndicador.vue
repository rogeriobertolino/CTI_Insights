<script setup>
defineProps({ rotulo: { type: String, required: true }, valor: { type: [String, Number], required: true }, detalhe: { type: String, default: '' } })
</script>
<template><article><span>{{ rotulo }}</span><b>{{ valor }}</b><small v-if="detalhe">{{ detalhe }}</small></article></template>
