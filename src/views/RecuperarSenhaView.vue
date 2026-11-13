<script setup>
import { ref } from 'vue'
import { authError, solicitarRedefinicaoSenha } from '../services/auth.js'

const email = ref('')
const mensagem = ref('')
const erro = ref('')
const carregando = ref(false)

async function solicitar() {
  if (carregando.value) return
  carregando.value = true
  erro.value = ''
  mensagem.value = ''
  try {
    const resposta = await solicitarRedefinicaoSenha(email.value)
    mensagem.value = resposta.message
  } catch (error) {
    erro.value = authError(error)
  } finally {
    carregando.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section>
      <RouterLink class="brand-light" to="/"><span>CTI</span><b>CTI Insights</b></RouterLink>
      <div class="login-box">
        <p class="kicker">RECUPERAÇÃO DE ACESSO</p>
        <h1>Recuperar senha.</h1>
        <p>Informe o e-mail associado à sua conta. Se estiver cadastrado, enviaremos um link de redefinição.</p>
        <form @submit.prevent="solicitar">
          <label>E-mail<input v-model="email" type="email" autocomplete="email" placeholder="nome@empresa.com.br" required maxlength="254"></label>
          <p v-if="erro" class="error" role="alert">{{ erro }}</p>
          <p v-if="mensagem" class="register-feedback" role="status">{{ mensagem }}</p>
          <button class="btn primary" type="submit" :disabled="carregando">{{ carregando ? 'Enviando…' : 'Enviar link de recuperação' }}</button>
        </form>
        <p class="auth-switch"><RouterLink to="/login">Voltar para o login</RouterLink></p>
      </div>
      <small>© 2026 CTI Insights</small>
    </section>
    <section class="login-art"><blockquote>Seu acesso continua protegido.</blockquote><p>CTI INSIGHTS — INTELIGÊNCIA COMERCIAL</p></section>
  </main>
</template>

<style scoped>
.auth-switch { text-align: center; margin-top: 20px; font-size: .78rem; color: var(--muted); }
.auth-switch a { color: var(--red); font-weight: 700; }
.register-feedback { background: #eef6fc; border-radius: 8px; padding: 12px; font-size: .8rem; line-height: 1.6; }
</style>