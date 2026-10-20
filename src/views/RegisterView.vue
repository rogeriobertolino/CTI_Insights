<script setup>
import { reactive, ref } from 'vue'
import { cadastrarUsuario, authError } from '../services/auth.js'

const form = reactive({ name: '', email: '', password: '', confirmation: '' })
const showPassword = ref(false)
const errors = ref({})
const message = ref('')
const loading = ref(false)
const created = ref(false)

async function submit() {
  if (loading.value || created.value) return
  message.value = ''
  const next = {}
  if (form.name.trim().length < 3) next.name = 'Informe seu nome com pelo menos 3 caracteres.'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) next.email = 'Informe um e-mail válido.'
  if (form.password.length < 8) next.password = 'Use pelo menos 8 caracteres.'
  else if (new TextEncoder().encode(form.password).length > 72) next.password = 'A senha está muito longa. Use uma senha menor.'
  if (!form.confirmation || form.password !== form.confirmation) next.confirmation = 'As senhas precisam ser iguais.'
  errors.value = next
  if (Object.keys(next).length) return
  loading.value = true
  try {
    await cadastrarUsuario(form)
    created.value = true
    message.value = 'Solicitação enviada. Aguarde a aprovação do administrador para entrar.'
    form.password = ''; form.confirmation = ''
  } catch (error) { message.value = authError(error) }
  finally { loading.value = false }
}
</script>

<template>
  <main class="login-page register-page">
    <section>
      <RouterLink class="brand-light" to="/"><span>CTI</span><b>CTI Insights</b></RouterLink>
      <div class="login-box register-box">
        <p class="kicker">CADASTRO DE USUÁRIO</p>
        <h1>Crie seu acesso.</h1>
        <p>Preencha seus dados para solicitar acesso ao CTI Insights. Seu cadastro será analisado pelo administrador.</p>

        <form novalidate @submit.prevent="submit">
          <label for="register-name">Nome completo
            <input id="register-name" v-model="form.name" name="name" autocomplete="name" placeholder="Seu nome completo" maxlength="120" required :aria-invalid="!!errors.name" :aria-describedby="errors.name ? 'name-error' : undefined">
            <small v-if="errors.name" id="name-error" class="field-error">{{ errors.name }}</small>
          </label>
          <label for="register-email">E-mail
            <input id="register-email" v-model="form.email" name="email" type="email" autocomplete="email" placeholder="nome@empresa.com.br" maxlength="254" required :aria-invalid="!!errors.email" :aria-describedby="errors.email ? 'email-error' : undefined">
            <small v-if="errors.email" id="email-error" class="field-error">{{ errors.email }}</small>
          </label>
          <label for="register-password">Senha
            <input id="register-password" v-model="form.password" name="password" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="No mínimo 8 caracteres" minlength="8" required :aria-invalid="!!errors.password" aria-describedby="password-help password-error">
            <small id="password-help" class="field-help">Use uma senha exclusiva, com pelo menos 8 caracteres.</small>
            <small v-if="errors.password" id="password-error" class="field-error">{{ errors.password }}</small>
          </label>
          <label for="register-confirmation">Confirmar senha
            <input id="register-confirmation" v-model="form.confirmation" name="confirmation" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="Digite a senha novamente" required :aria-invalid="!!errors.confirmation" :aria-describedby="errors.confirmation ? 'confirmation-error' : undefined">
            <small v-if="errors.confirmation" id="confirmation-error" class="field-error">{{ errors.confirmation }}</small>
          </label>
          <label class="remember"><input v-model="showPassword" type="checkbox"> Mostrar senhas</label>
          <p v-if="Object.keys(errors).length" class="field-error" role="alert">Revise os campos indicados para continuar.</p>
          <p v-if="message" class="register-feedback" role="status">{{ message }}</p>
          <button class="btn primary" type="submit" :disabled="loading || created">{{ loading ? 'Criando conta…' : created ? 'Conta criada' : 'Criar conta →' }}</button>
        </form>
        <p class="auth-switch">Já tem uma conta? <RouterLink to="/login">Entrar</RouterLink></p>
      </div>
      <small>© 2026 CTI Insights</small>
    </section>
    <section class="login-art">
      <blockquote>Uma nova perspectiva para os seus dados começa aqui.</blockquote>
      <p>CTI INSIGHTS — INTELIGÊNCIA COMERCIAL</p>
    </section>
  </main>
</template>

<style scoped>
.register-box { padding: 36px 0; width: 100%; }
.register-box h1 { margin: 18px 0; }
.register-note { background: #eef6fc; border-radius: 8px; padding: 12px; line-height: 1.5; }
.field-help { color: var(--muted); font-weight: 400; line-height: 1.5; }
.field-error { color: #b42318; font-size: .75rem; font-weight: 500; margin: 0; }
input[aria-invalid="true"] { border-color: #b42318; }
input:focus-visible, button:focus-visible, a:focus-visible { outline: 3px solid #006eb7; outline-offset: 3px; }
.register-feedback { background: #eef6fc; border-radius: 8px; padding: 12px; font-size: .8rem; line-height: 1.6; }
.auth-switch { text-align: center; margin-top: 20px; }
.auth-switch a { color: var(--red); font-weight: 700; }
.register-page .btn:disabled { opacity: .65; }
@media (max-width: 480px) { .register-page > section:first-child { padding: 24px; } .register-box h1 { font-size: 2.5rem; } }
</style>
