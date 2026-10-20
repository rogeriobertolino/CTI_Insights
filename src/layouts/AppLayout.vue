<script setup>
import { useAuthStore } from '../stores/auth.js'
const auth = useAuthStore()
import Header from '../components/Header.vue'
import Sidebar from '../components/Sidebar.vue'
</script>

<template>
  <div class="app-layout">
    <a class="skip-link" href="#conteudo">Pular para o conteúdo</a>
    <Sidebar />
    <div class="app-shell"><Header /><nav class="mobile-nav" aria-label="Validação de planilha"><RouterLink to="/app/upload">Validar planilha</RouterLink><RouterLink to="/app/relatorios">Relatório</RouterLink><RouterLink to="/app/importacao-servidor">Importação</RouterLink><RouterLink to="/app/historico">Histórico</RouterLink><RouterLink v-if="auth.usuario?.perfil === 'ADMIN'" to="/app/admin/cadastros">Cadastros</RouterLink><RouterLink v-if="auth.usuario?.perfil === 'ADMIN'" to="/app/gestao">Gestão</RouterLink></nav><main id="conteudo" class="app-content"><RouterView /></main></div>
  </div>
</template>

