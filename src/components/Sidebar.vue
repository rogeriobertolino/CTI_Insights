<template>
  <aside class="sidebar">
    <RouterLink class="brand" to="/"><span>CTI</span><p><strong>CTI Insights</strong><small>Dados que orientam</small></p></RouterLink>
    <nav aria-label="Navegação principal">
      <small>WORKSPACE</small>
      <RouterLink to="/app/graficos"><b>▥</b> Dashboard</RouterLink>
      <RouterLink to="/app/upload"><b>⇧</b> Importar dados</RouterLink>
      <RouterLink to="/app/relatorios"><b>▤</b> Relatórios</RouterLink>
      <small>GOVERNANÇA</small>
      <a href="#" @click.prevent><b>◇</b> Qualidade <i>12</i></a>
      <a href="#" @click.prevent><b>◫</b> Auditoria</a>
    </nav>
    <footer><span>● Sistema operacional</span><small>Versão 1.0.0 · MVP</small></footer>
  </aside>
</template>
