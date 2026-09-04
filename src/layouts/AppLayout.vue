<script setup>
import Header from '../components/Header.vue'
import Sidebar from '../components/Sidebar.vue'
</script>

<template>
  <div class="app-layout">
    <a class="skip-link" href="#conteudo">Pular para o conteúdo</a>
    <Sidebar />
    <div class="app-shell"><Header /><main id="conteudo" class="app-content"><RouterView /></main></div>
  </div>
</template>

