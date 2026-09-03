<template>
  <main>
    <h1>Entrar</h1>
    <form @submit.prevent="$router.push('/app')">
      <label>E-mail <input type="email" required /></label>
      <label>Senha <input type="password" required /></label>
      <button type="submit">Acessar painel</button>
    </form>
  </main>
</template>