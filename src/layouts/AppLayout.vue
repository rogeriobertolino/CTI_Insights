<script setup>
import Header from '../components/Header.vue'
import Sidebar from '../components/Sidebar.vue'
</script>

<template>
  <div class="app-layout">
    <a class="skip-link" href="#conteudo">Pular para o conteúdo</a>
    <Sidebar />
    <div class="app-shell"><Header /><nav class="mobile-nav" aria-label="Validação de planilha"><RouterLink to="/app/upload">Validar planilha</RouterLink><RouterLink to="/app/relatorios">Relatório</RouterLink></nav><main id="conteudo" class="app-content"><RouterView /></main></div>
  </div>
</template>

