<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
const auth = useAuthStore()
const router = useRouter()
const email = ref('')
const senha = ref('')
async function login() {
  if (await auth.login(email.value, senha.value)) {
    senha.value = ''
    router.push(auth.usuario.perfil === 'ADMIN' ? '/app/admin/cadastros' : '/app/upload')
  }
}
</script>
<template>
  <main class="login-page">
    <section>
      <RouterLink class="brand-light" to="/"><span>CTI</span><b>CTI Insights</b></RouterLink>
      <div class="login-box">
        <p class="kicker">ACESSO À PLATAFORMA</p><h1>Bem-vindo de volta.</h1>
        <p>Entre com seu e-mail e senha para salvar análises e acessar o dashboard.</p>
        <form @submit.prevent="login">
          <label>E-mail<input v-model="email" type="email" autocomplete="username" placeholder="nome@empresa.com.br" required></label>
          <label>Senha<input v-model="senha" type="password" autocomplete="current-password" placeholder="Sua senha" required></label>
          <p v-if="auth.erro" class="error" role="alert">{{ auth.erro }}</p>
          <button class="btn primary" :disabled="auth.carregando">{{ auth.carregando ? 'Entrando…' : 'Acessar plataforma →' }}</button>
        </form>
        <p class="auth-switch">Ainda não tem conta? <RouterLink to="/cadastro">Cadastre-se</RouterLink></p>
        <p class="auth-switch"><RouterLink to="/app/upload">Validar planilha sem entrar</RouterLink></p>
      </div>
      <small>© 2026 CTI Insights</small>
    </section>
    <section class="login-art"><blockquote>Dados confiáveis são a base para decisões que movem o negócio.</blockquote><p>CTI INSIGHTS — INTELIGÊNCIA COMERCIAL</p></section>
  </main>
</template>
<style scoped>
.auth-switch { text-align:center; margin-top:20px; font-size:.78rem; color:var(--muted); }
.auth-switch a { color:var(--red); font-weight:700; }
</style>
