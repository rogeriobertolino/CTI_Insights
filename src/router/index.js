import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '../views/HomeView.vue'
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
    path: '/app',
    component: AppLayout,
    redirect: '/app/upload',
    children: [
      { path: 'upload', name: 'upload', component: UploadView },
      { path: 'relatorios', name: 'relatorios', component: RelatoriosView },
      { path: 'graficos', name: 'graficos', component: GraficosView },
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

export default router