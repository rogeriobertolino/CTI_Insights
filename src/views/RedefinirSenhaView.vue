<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { authError, redefinirSenha } from '../services/auth.js'

const route = useRoute()
const token = computed(() => typeof route.query.token === 'string' ? route.query.token : '')
const senha = ref('')
const confirmacao = ref('')
const mensagem = ref('')
const erro = ref('')
const carregando = ref(false)

async function salvar() {
  erro.value = ''
  mensagem.value = ''
  if (!token.value) {
    erro.value = 'O link está incompleto. Solicite uma nova redefinição.'
    return
  }
  if (senha.value !== confirmacao.value) {
    erro.value = 'As senhas não coincidem.'
    return
  }
  if (new TextEncoder().encode(senha.value).length > 72) {
    erro.value = 'A senha deve ter no máximo 72 bytes.'
    return
  }
  carregando.value = true
  try {
    await redefinirSenha(token.value, senha.value)
    mensagem.value = 'Senha alterada. Agora você já pode entrar com a nova senha.'
    senha.value = ''
    confirmacao.value = ''
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
        <p class="kicker">NOVA SENHA</p>
        <h1>Redefinir senha.</h1>
        <form @submit.prevent="salvar">
          <label>Nova senha<input v-model="senha" type="password" autocomplete="new-password" minlength="8" maxlength="72" placeholder="No mínimo 8 caracteres" required></label>
          <label>Confirmar senha<input v-model="confirmacao" type="password" autocomplete="new-password" minlength="8" maxlength="72" placeholder="Digite novamente" required></label>
          <p v-if="erro" class="error" role="alert">{{ erro }}</p>
          <p v-if="mensagem" class="register-feedback" role="status">{{ mensagem }} <RouterLink to="/login">Entrar</RouterLink></p>
          <button class="btn primary" type="submit" :disabled="carregando || !!mensagem">{{ carregando ? 'Salvando…' : 'Salvar nova senha' }}</button>
        </form>
        <p class="auth-switch"><RouterLink to="/login">Voltar para o login</RouterLink></p>
      </div>
      <small>© 2026 CTI Insights</small>
    </section>
    <section class="login-art"><blockquote>Uma senha nova, com a mesma proteção.</blockquote><p>CTI INSIGHTS — INTELIGÊNCIA COMERCIAL</p></section>
  </main>
</template>

<style scoped>
.auth-switch { text-align: center; margin-top: 20px; font-size: .78rem; color: var(--muted); }
.auth-switch a, .register-feedback a { color: var(--red); font-weight: 700; }
.register-feedback { background: #eef6fc; border-radius: 8px; padding: 12px; font-size: .8rem; line-height: 1.6; }
</style>