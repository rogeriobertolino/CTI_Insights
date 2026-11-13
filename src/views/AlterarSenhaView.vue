<script setup>
import { ref } from 'vue'
import { alterarSenha, authError } from '../services/auth.js'

const senhaAtual = ref('')
const novaSenha = ref('')
const confirmacao = ref('')
const mensagem = ref('')
const erro = ref('')
const carregando = ref(false)

async function salvar() {
  erro.value = ''
  mensagem.value = ''
  if (novaSenha.value !== confirmacao.value) {
    erro.value = 'As senhas não coincidem.'
    return
  }
  if (novaSenha.value === senhaAtual.value) {
    erro.value = 'A nova senha deve ser diferente da atual.'
    return
  }
  if (new TextEncoder().encode(novaSenha.value).length > 72) {
    erro.value = 'A senha deve ter no máximo 72 bytes.'
    return
  }
  carregando.value = true
  try {
    await alterarSenha(senhaAtual.value, novaSenha.value)
    mensagem.value = 'Senha alterada. Use a nova senha no próximo login.'
    senhaAtual.value = ''
    novaSenha.value = ''
    confirmacao.value = ''
  } catch (error) {
    erro.value = authError(error)
  } finally {
    carregando.value = false
  }
}
</script>

<template>
  <section class="page">
    <div class="page-head"><div><p class="kicker">MINHA CONTA</p><h1>Alterar senha</h1><span>Informe a senha atual e escolha uma nova.</span></div></div>
    <article class="panel senha-panel">
      <form @submit.prevent="salvar">
        <label>Senha atual<input v-model="senhaAtual" type="password" autocomplete="current-password" maxlength="72" required></label>
        <label>Nova senha<input v-model="novaSenha" type="password" autocomplete="new-password" minlength="8" maxlength="72" placeholder="No mínimo 8 caracteres" required></label>
        <label>Confirmar nova senha<input v-model="confirmacao" type="password" autocomplete="new-password" minlength="8" maxlength="72" placeholder="Digite novamente" required></label>
        <p v-if="erro" class="error" role="alert">{{ erro }}</p>
        <p v-if="mensagem" class="register-feedback" role="status">{{ mensagem }}</p>
        <button class="btn primary" type="submit" :disabled="carregando">{{ carregando ? 'Salvando…' : 'Salvar nova senha' }}</button>
      </form>
    </article>
  </section>
</template>

<style scoped>
.senha-panel { max-width: 460px; }
.senha-panel form { display: grid; gap: 14px; }
.senha-panel label { display: grid; gap: 6px; font-size: .8rem; font-weight: 600; }
.senha-panel input { padding: 10px 12px; border: 1px solid #d5dde5; border-radius: 8px; font: inherit; }
.error { color: var(--red); font-size: .8rem; }
.register-feedback { background: #eef6fc; border-radius: 8px; padding: 12px; font-size: .8rem; line-height: 1.6; }
</style>
