<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
const loading=ref(false),router=useRouter()
const login=()=>{loading.value=true;setTimeout(()=>router.push('/app/graficos'),500)}
</script>

<template>
  <main class="login-page">
    <section><RouterLink class="brand-light" to="/"><span>CTI</span><b>CTI Insights</b></RouterLink><div class="login-box"><p class="kicker">ACESSO SEGURO</p><h1>Bem-vindo de volta.</h1><p>Entre com suas credenciais corporativas para acessar a inteligência comercial.</p><form @submit.prevent="login"><label>E-mail corporativo<input type="email" autocomplete="username" placeholder="nome@empresa.com.br" required></label><label>Senha<input type="password" autocomplete="current-password" placeholder="Sua senha" required></label><label class="remember"><input type="checkbox"> Manter conectado</label><button class="btn primary" :disabled="loading">{{ loading?'Autenticando...':'Acessar plataforma →' }}</button></form><aside>✓ <span><b>Ambiente protegido</b><small>Sua sessão e suas ações são registradas com segurança.</small></span></aside></div><small>© 2026 CTI Insights · Privacidade e segurança</small></section>
    <section class="login-art"><blockquote>“<br>Dados confiáveis não são apenas números. São a base para decisões que movem o negócio.</blockquote><p>CTI INSIGHTS — INTELIGÊNCIA COMERCIAL</p></section>
  </main>
</template>
