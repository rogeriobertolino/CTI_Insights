import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { useAuthStore } from './stores/auth.js'
import { useUploadStore } from './stores/uploadStore.js'
import { useCadastrosStore } from './stores/cadastros.js'
import { clearData } from './dataStore.js'
import App from './App.vue' // Importa o componente raiz da aplicação
import router from './router/index.js' // Importa as rotas de src/router/index.js
import './style.css' // Importa o Tailwind CSS
import './validation.css'

const app = createApp(App)

app.use(createPinia()) // Ativa as stores antes de montar a aplicação
app.use(router) // Registra o Vue Router na aplicação
window.addEventListener('cti:auth-expired', () => {
  useAuthStore().$reset()
  clearData()
  useCadastrosStore().limpar()
  useUploadStore().limpar()
  router.push('/login')
})
app.mount('#app') // Monta a aplicação no elemento com id "app"
