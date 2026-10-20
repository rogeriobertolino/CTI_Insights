<script setup>
import { computed, ref, watch } from 'vue'
import { useUploadStore } from '../stores/uploadStore.js'

const store = useUploadStore()
const pagina = ref(1)
const paginas = computed(() => Math.max(1, Math.ceil(store.clientesFiltrados.length / 25)))
const clientes = computed(() => store.clientesFiltrados.slice((pagina.value - 1) * 25, pagina.value * 25))
watch(() => [store.validadoEm, store.filtroSegmento], () => { pagina.value = 1 })
</script>

<template>
  <article v-if="store.temDados" class="panel">
    <div class="validation-section-head"><h2>Prévia dos clientes tratados</h2><span>Total: {{ store.totalClientes }} · Clientes nível A: {{ store.clientesNivelA }} · Ocorrências: {{ store.totalErros }}</span></div>
    <p class="validation-copy">Espaços removidos, segmentos padronizados e níveis em maiúsculas. Os dados originais continuam disponíveis no relatório; as ocorrências apontam problemas encontrados na origem.</p>
    <div class="validation-counts"><div v-for="(quantidade, segmento) in store.resumoPorSegmento" :key="segmento"><span>{{ segmento }}</span><b>{{ quantidade }}</b></div></div>
    <div class="filters"><label for="preview-segmento">Segmento</label><select id="preview-segmento" v-model="store.filtroSegmento"><option>Todos</option><option v-for="segmento in store.segmentos" :key="segmento">{{ segmento }}</option></select></div>
    <div v-if="store.cabecalhos.includes('faturamento')" class="validation-counts"><div><span>Faturamento total {{ store.periodoFaturamento }} · registros válidos</span><b>{{ store.totalFaturamento.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) }}</b></div><div><span>Faturamento médio · registros válidos</span><b>{{ store.faturamentoMedio.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) }}</b></div></div>
    <div class="table-wrap"><table class="validation-table"><caption class="sr-only">Clientes após o tratamento inicial da planilha</caption><thead><tr><th scope="col">Linha</th><th scope="col">Código</th><th scope="col">Nome</th><th scope="col">Consultor</th><th scope="col">Segmento</th><th scope="col">Nível</th><th v-if="store.cabecalhos.includes('faturamento')" scope="col">Faturamento</th><th scope="col">Validação</th></tr></thead><tbody><tr v-for="cliente in clientes" :key="cliente.numero_linha"><td>{{ cliente.numero_linha }}</td><td>{{ cliente.codigo_cliente }}</td><td>{{ cliente.nome_cliente }}</td><td>{{ cliente.consultor || '—' }}</td><td>{{ cliente.segmento || '—' }}</td><td>{{ cliente.nivel_cliente || '—' }}</td><td v-if="store.cabecalhos.includes('faturamento')">{{ cliente.faturamento == null ? '—' : cliente.faturamento.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) }}</td><td><span class="validation-badge" :class="{ invalid: cliente.erros.length > 0 }">{{ cliente.erros.length ? 'Revisar' : 'Válido' }}</span></td></tr></tbody></table></div>
    <nav class="validation-pagination" aria-label="Páginas dos clientes tratados"><button class="btn outline" :disabled="pagina <= 1" @click="pagina--">Anterior</button><span>Página {{ pagina }} de {{ paginas }}</span><button class="btn outline" :disabled="pagina >= paginas" @click="pagina++">Próxima</button></nav>
  </article>
</template>
