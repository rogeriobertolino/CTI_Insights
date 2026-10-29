<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { clearData } from '../stores/dataStore.js'
import { useUploadStore } from '../stores/uploadStore.js'
const auth = useAuthStore()
const router = useRouter()
const initials = computed(() => auth.usuario?.nome.split(/\s+/).slice(0,2).map(s=>s[0]).join('').toUpperCase() || 'CTI')
onMounted(() => auth.restaurarSessao())
async function logout() {
  if (await auth.logout()) {
    clearData()
    useUploadStore().limpar()
    router.push('/login')
  }
}
</script>
<template>
  <header class="app-header">
    <div><small>VISÃO GERAL</small><strong>Central de inteligência comercial</strong></div>
    <div class="user" v-if="auth.usuario"><span class="avatar">{{ initials }}</span><p><b>{{ auth.usuario.nome }}</b><small>{{ auth.usuario.perfil }}</small></p><button :disabled="auth.carregando" @click="logout">Sair</button><small v-if="auth.erro" role="alert">{{ auth.erro }}</small></div>
    <RouterLink v-else class="btn outline" to="/login">Entrar</RouterLink>
  </header>
</template>
