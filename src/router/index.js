import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '../views/HomeView.vue'
import { useAuthStore } from '../stores/auth.js'
import LoginView from '../views/LoginView.vue'
import AppLayout from '../layouts/AppLayout.vue'

const UploadView = () => import('../views/UploadView.vue')
const RelatoriosView = () => import('../views/RelatoriosView.vue')
const GraficosView = () => import('../views/GraficosView.vue')

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
  },
  {
    path: '/cadastro',
    name: 'cadastro',
    component: () => import('../views/RegisterView.vue'),
  },
  {
    path: '/app',
    component: AppLayout,
    redirect: '/app/upload',
    children: [
      { path: 'gestao', name: 'gestao-comercial', component: () => import('../views/GestaoComercialView.vue'), meta: { requiresAdmin: true } },
      { path: 'admin/cadastros', name: 'admin-cadastros', component: () => import('../views/AdminCadastrosView.vue'), meta: { requiresAdmin: true } },
      { path: 'upload', name: 'upload', component: UploadView },
      { path: 'relatorios', name: 'relatorios', component: RelatoriosView },
      { path: 'historico', name: 'historico-importacoes', component: () => import('../views/HistoricoImportacoesView.vue') },
      { path: 'graficos', name: 'graficos', component: GraficosView },
      { path: 'importacao-servidor', name: 'importacao-servidor', component: () => import('../views/ImportacaoServidorView.vue') },
      { path: 'registros-servidor', name: 'registros-servidor', component: () => import('../views/RelatorioServidorView.vue') },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(async to => {
  if (!to.meta.requiresAdmin && !['graficos', 'registros-servidor', 'historico-importacoes'].includes(to.name)) return true
  const auth = useAuthStore()
  await auth.restaurarSessao()
  if (!auth.usuario) return { name: 'login' }
  if (to.meta.requiresAdmin && auth.usuario.perfil !== 'ADMIN') return { name: 'upload' }
  return true
})
export default router
