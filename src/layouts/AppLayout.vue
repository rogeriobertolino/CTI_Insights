<script setup>
import Header from '../components/Header.vue'
import Sidebar from '../components/Sidebar.vue'
</script>

<template>
  <div class="app-layout">
    <Header />
    <div class="app-layout__body">
      <Sidebar />
      <main class="app-layout__content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-layout__body { display: flex; min-height: calc(100vh - 64px); }
.app-layout__content { flex: 1; padding: 32px; }
</style>