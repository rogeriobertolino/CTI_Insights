<script setup>
import { ref } from 'vue'

const isOpen = ref(true)
</script>

<template>
  <aside :class="['sidebar', { 'sidebar--collapsed': !isOpen }]">
    <button class="menu-button" type="button" aria-label="Alternar menu" @click="isOpen = !isOpen">
      ☰
    </button>
    <nav v-if="isOpen" aria-label="Navegação principal">
      <RouterLink to="/app/upload">Upload</RouterLink>
      <RouterLink to="/app/relatorios">Relatórios</RouterLink>
      <RouterLink to="/app/graficos">Gráficos</RouterLink>
    </nav>
  </aside>
</template>

<style scoped>
.sidebar { background: #172033; color: #fff; min-height: calc(100vh - 64px); padding: 16px; width: 220px; }
.sidebar--collapsed { width: 56px; }
.menu-button { background: transparent; border: 0; color: inherit; cursor: pointer; font-size: 24px; }
nav { display: grid; gap: 12px; margin-top: 24px; }
a { color: inherit; text-decoration: none; }
.router-link-active { font-weight: 700; }
</style>