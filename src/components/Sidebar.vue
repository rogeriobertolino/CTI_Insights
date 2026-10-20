<script setup>
import { useAuthStore } from '../stores/auth.js'
const auth = useAuthStore()
</script>
<template>
  <aside class="sidebar">
    <RouterLink class="brand" to="/"><span>CTI</span><p><strong>CTI Insights</strong><small>Dados que orientam</small></p></RouterLink>
    <nav aria-label="Navegação principal">
      <small>VALIDAÇÃO DE PLANILHA</small>
      <RouterLink to="/app/upload"><b>⇧</b> Validar planilha</RouterLink>
      <RouterLink to="/app/relatorios"><b>▤</b> Relatório de validação</RouterLink>
      <small>ANÁLISES NO SERVIDOR</small>
      <RouterLink to="/app/graficos"><b>▥</b> Dashboard</RouterLink>
      <RouterLink to="/app/historico"><b>▤</b> Histórico de importações</RouterLink>
      <RouterLink to="/app/importacao-servidor"><b>⇧</b> Importação integrada</RouterLink>
      <RouterLink to="/app/registros-servidor"><b>▤</b> Registros persistidos</RouterLink>
      <template v-if="auth.usuario?.perfil === 'ADMIN'">
        <small>ADMINISTRAÇÃO</small>
        <RouterLink to="/app/gestao"><b>▤</b> Gestão comercial</RouterLink>
        <RouterLink to="/app/admin/cadastros"><b>✓</b> Autorizar cadastros</RouterLink>
      </template>
    </nav>
    <footer><span>● Sistema operacional</span><small>Versão 1.0.0 · MVP</small></footer>
  </aside>
</template>
