<script setup>
import { onMounted } from 'vue'
import { useCadastrosStore } from '../stores/cadastros.js'
const store = useCadastrosStore()
const rotulos = { PENDENTE: 'Pendente', APROVADO: 'Aprovado', RECUSADO: 'Recusado' }
const data = valor => valor ? new Date(valor).toLocaleString('pt-BR') : '—'
onMounted(() => store.carregar('PENDENTE', 0))
</script>

<template>
  <section class="page validation-page admin-cadastros">
    <div class="page-head">
      <div><p class="kicker">ADMINISTRAÇÃO</p><h1>Autorizar cadastros</h1><span>Revise as solicitações de acesso ao CTI Insights.</span></div>
      <button class="btn outline" :disabled="store.carregando || store.revisando !== null" @click="store.carregar()">Atualizar lista</button>
    </div>
    <div class="kpis admin-kpis">
      <article v-for="(rotulo, estado) in rotulos" :key="estado"><span>{{ rotulo }}s</span><b>{{ store.totais[estado] }}</b></article>
    </div>
    <article class="panel">
      <div class="validation-section-head">
        <div><h2>Solicitações de cadastro</h2><p class="validation-copy">O usuário poderá entrar e salvar análises após a sua aprovação.</p></div>
        <label class="admin-filter" for="cadastro-status">Situação
          <select id="cadastro-status" :value="store.filtro" :disabled="store.carregando || store.revisando !== null" @change="store.carregar($event.target.value, 0)">
            <option value="PENDENTE">Pendentes</option><option value="APROVADO">Aprovados</option><option value="RECUSADO">Recusados</option><option value="">Todos</option>
          </select>
        </label>
      </div>
      <p v-if="store.erro" class="validation-alert" role="alert">{{ store.erro }}</p>
      <p v-if="store.mensagem" class="validation-success" role="status">{{ store.mensagem }}</p>
      <p v-if="store.carregando" class="validation-copy" role="status">Carregando cadastros…</p>
      <div v-else-if="!store.usuarios.length && !store.erro" class="empty-state" role="status">
        <b>{{ store.filtro === 'PENDENTE' ? 'Nenhum cadastro aguardando aprovação' : 'Nenhum cadastro nesta situação' }}</b>
        <p>Use os filtros ou atualize a lista para consultar novas solicitações.</p>
      </div>
      <div v-else-if="store.usuarios.length" class="table-wrap">
        <table class="validation-table">
          <caption class="sr-only">Usuários e decisões de autorização de cadastro</caption>
          <thead><tr><th scope="col">Nome e e-mail</th><th scope="col">Solicitado em</th><th scope="col">Situação</th><th scope="col">Revisão</th><th scope="col">Ações</th></tr></thead>
          <tbody>
            <tr v-for="usuario in store.usuarios" :key="usuario.id">
              <td><strong>{{ usuario.nome }}</strong><span>{{ usuario.email }}</span></td>
              <td>{{ data(usuario.cadastradoEm) }}</td>
              <td><span class="validation-badge" :class="{ invalid: usuario.statusCadastro === 'RECUSADO', pending: usuario.statusCadastro === 'PENDENTE' }">{{ rotulos[usuario.statusCadastro] }}</span></td>
              <td><template v-if="usuario.revisadoEm"><span>{{ usuario.revisadoPor }}</span><small>{{ data(usuario.revisadoEm) }}</small></template><span v-else>—</span></td>
              <td>
                <div v-if="usuario.statusCadastro === 'PENDENTE'" class="admin-actions">
                  <button class="btn primary" :aria-label="'Aprovar cadastro de ' + usuario.nome" :disabled="store.revisando !== null" @click="store.revisar(usuario.id, 'APROVADO')">{{ store.revisando === usuario.id ? 'Processando…' : 'Aprovar' }}</button>
                  <button class="btn outline refuse" :aria-label="'Recusar cadastro de ' + usuario.nome" :disabled="store.revisando !== null" @click="store.revisar(usuario.id, 'RECUSADO')">Recusar</button>
                </div>
                <span v-else class="validation-copy">Analisado</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <nav v-if="store.totalPaginas > 1" class="validation-pagination" aria-label="Páginas dos cadastros">
        <button class="btn outline" :disabled="store.pagina === 0 || store.carregando || store.revisando !== null" @click="store.carregar(store.filtro, store.pagina - 1)">Anterior</button>
        <span>Página {{ store.pagina + 1 }} de {{ store.totalPaginas }}</span>
        <button class="btn outline" :disabled="store.pagina + 1 >= store.totalPaginas || store.carregando || store.revisando !== null" @click="store.carregar(store.filtro, store.pagina + 1)">Próxima</button>
      </nav>
    </article>
  </section>
</template>

<style scoped>
.admin-kpis { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.admin-filter { display: grid; gap: 8px; color: #41525d; font-size: .85rem; }
.admin-filter select { border: 1px solid var(--border); border-radius: 7px; padding: 9px 12px; background: white; }
.admin-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.refuse { color: #a12e36; }
.validation-badge.pending { color: #795006; background: #fff4d9; }
.validation-success { margin-bottom: 16px; }
@media (max-width: 700px) { .admin-kpis { grid-template-columns: 1fr; } .admin-filter { width: 100%; } }
</style>
