<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { listarComercial, salvarComercial, excluirComercial } from '../services/comercial.js'
import { authError } from '../services/auth.js'
import { cadastrosComerciais as configuracoes, formularioComercial, payloadComercial } from '../utils/cadastrosComerciais.js'

const recurso = ref('consultores')
const config = computed(() => configuracoes[recurso.value])
const registros = ref([])
const pagina = ref(0)
const totalPaginas = ref(0)
const total = ref(0)
const carregando = ref(false)
const salvando = ref(false)
const erro = ref('')
const mensagem = ref('')
const editando = ref(false)
const id = ref(null)
const formulario = ref({})
const exclusao = ref(null)
const filtroSegmento = ref('')
const filtroNivel = ref('')
const filtroConsultor = ref('')
let versao = 0
let ativa = true
onUnmounted(() => { ativa = false; versao++ })

async function carregar(destino = 0) {
  const atual = ++versao
  carregando.value = true; erro.value = ''
  try {
    const filtros = recurso.value === 'clientes' ? {
      ...(filtroSegmento.value ? { segmento: filtroSegmento.value } : {}),
      ...(filtroNivel.value ? { nivel: filtroNivel.value } : {}),
      ...(filtroConsultor.value ? { consultorId: filtroConsultor.value } : {}),
    } : {}
    const resultado = await listarComercial(recurso.value, destino, filtros)
    if (atual !== versao || !ativa) return
    if (resultado.totalElements && destino >= resultado.totalPages) return await carregar(resultado.totalPages - 1)
    registros.value = resultado.content; pagina.value = resultado.number
    totalPaginas.value = resultado.totalPages; total.value = resultado.totalElements
  } catch (error) {
    if (atual === versao && ativa) { registros.value = []; erro.value = authError(error) }
  } finally { if (atual === versao && ativa) carregando.value = false }
}
function trocar() {
  registros.value = []; editando.value = false; exclusao.value = null; mensagem.value = ''
  filtroSegmento.value = ''; filtroNivel.value = ''; filtroConsultor.value = ''
  carregar()
}
function editar(registro = null) {
  id.value = registro?.id ?? null
  formulario.value = formularioComercial(recurso.value, registro || {})
  editando.value = true; erro.value = ''; mensagem.value = ''; exclusao.value = null
}
async function salvar() {
  if (salvando.value) return
  erro.value = ''; salvando.value = true
  try {
    const dados = payloadComercial(recurso.value, formulario.value)
    if (dados.dataFim && dados.dataFim < dados.dataInicio) throw new Error('A data final não pode ser anterior à inicial.')
    await salvarComercial(recurso.value, dados, id.value)
    if (!ativa) return
    editando.value = false; mensagem.value = 'Registro salvo.'
    await carregar(pagina.value)
  } catch (error) { if (ativa) erro.value = authError(error) }
  finally { if (ativa) salvando.value = false }
}
async function excluir() {
  if (salvando.value || !exclusao.value) return
  salvando.value = true; erro.value = ''
  try {
    await excluirComercial(recurso.value, exclusao.value.id)
    if (!ativa) return
    exclusao.value = null; mensagem.value = 'Registro excluído.'
    await carregar(pagina.value)
  } catch (error) { if (ativa) erro.value = authError(error) }
  finally { if (ativa) salvando.value = false }
}
function texto(valor, campo) {
  if (valor == null || valor === '') return '—'
  if (campo.tipo === 'moeda') return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
  return String(valor)
}
onMounted(() => carregar())
</script>

<template>
  <section class="page validation-page gestao">
    <div class="page-head"><div><p class="kicker">ADMINISTRAÇÃO</p><h1>Gestão comercial</h1><span>Cadastros, vínculos e histórico de importações.</span></div></div>
    <article class="panel">
      <div class="validation-section-head">
        <label class="recurso">Consultar
          <select v-model="recurso" :disabled="salvando" @change="trocar"><option v-for="(item, chave) in configuracoes" :key="chave" :value="chave">{{ item.titulo }}</option></select>
        </label>
        <div class="acoes"><button class="btn outline" :disabled="carregando || salvando" @click="carregar(pagina)">Atualizar</button><button v-if="!config.somenteLeitura" class="btn primary" :disabled="salvando" @click="editar()">Novo registro</button></div>
      </div>
      <p v-if="erro" class="validation-alert" role="alert">{{ erro }}</p>
      <p v-if="mensagem" class="validation-success" role="status">{{ mensagem }}</p>
      <form v-if="recurso === 'clientes' && !editando" class="filtros" @submit.prevent="carregar()">
        <label>Segmento<input v-model="filtroSegmento" maxlength="100"></label>
        <label>Nível<select v-model="filtroNivel"><option value="">Todos</option><option>A</option><option>B</option><option>C</option></select></label>
        <label>ID do consultor<input v-model="filtroConsultor" type="number" min="1"></label>
        <button class="btn outline" :disabled="carregando || salvando">Filtrar</button>
      </form>
      <form v-if="editando" class="editor" @submit.prevent="salvar">
        <h2>{{ id === null ? 'Novo registro' : 'Editar registro #' + id }}</h2>
        <p v-if="['clientes', 'contratos', 'insights'].includes(recurso)" class="validation-copy">Os IDs podem ser consultados nas tabelas de clientes, consultores, serviços e contratos desta página.</p>
        <div class="campos">
          <label v-for="campo in config.campos" :key="campo.nome">{{ campo.rotulo }}{{ campo.obrigatorio ? ' *' : '' }}
            <select v-if="campo.opcoes" v-model="formulario[campo.nome]" :required="campo.obrigatorio" :disabled="salvando"><option v-for="opcao in campo.opcoes" :key="opcao">{{ opcao }}</option></select>
            <textarea v-else-if="campo.tipo === 'texto'" v-model="formulario[campo.nome]" :required="campo.obrigatorio" :maxlength="campo.max" :disabled="salvando" rows="3"></textarea>
            <input v-else v-model="formulario[campo.nome]" :type="['id', 'moeda'].includes(campo.tipo) ? 'number' : campo.tipo || 'text'" :min="campo.tipo === 'id' ? 1 : campo.tipo === 'moeda' ? campo.min ?? 0 : undefined" :step="campo.tipo === 'moeda' ? '0.01' : campo.tipo === 'id' ? '1' : undefined" :maxlength="campo.max" :required="campo.obrigatorio" :disabled="salvando">
          </label>
        </div>
        <div class="acoes"><button type="button" class="btn outline" :disabled="salvando" @click="editando = false">Cancelar</button><button class="btn primary" :disabled="salvando">{{ salvando ? 'Salvando…' : 'Salvar' }}</button></div>
      </form>
      <div v-if="exclusao" class="validation-alert" role="alert">
        <p>Excluir o registro #{{ exclusao.id }}? Registros vinculados a outros cadastros serão preservados.</p>
        <div class="acoes"><button class="btn outline" :disabled="salvando" @click="exclusao = null">Cancelar</button><button class="btn primary" :disabled="salvando" @click="excluir">Confirmar exclusão</button></div>
      </div>
      <p v-if="carregando" role="status">Carregando registros…</p>
      <template v-else>
        <div class="validation-section-head"><h2>{{ config.titulo }}</h2><span>{{ total }} registro(s)</span></div>
        <p v-if="!registros.length && !erro" class="validation-copy">Nenhum registro encontrado.</p>
        <div v-else-if="registros.length" class="table-wrap"><table class="validation-table">
          <caption class="sr-only">{{ config.titulo }}</caption>
          <thead><tr><th scope="col">ID</th><th v-for="campo in config.campos" :key="campo.nome" scope="col">{{ campo.rotulo }}</th><th v-if="!config.somenteLeitura" scope="col">Ações</th></tr></thead>
          <tbody><tr v-for="registro in registros" :key="registro.id"><td>{{ registro.id }}</td><td v-for="campo in config.campos" :key="campo.nome">{{ texto(registro[campo.nome], campo) }}</td><td v-if="!config.somenteLeitura"><div class="acoes"><button class="btn outline" :disabled="salvando" :aria-label="'Editar registro ' + registro.id" @click="editar(registro)">Editar</button><button class="btn outline" :disabled="salvando" :aria-label="'Excluir registro ' + registro.id" @click="exclusao = registro">Excluir</button></div></td></tr></tbody>
        </table></div>
        <nav v-if="totalPaginas > 1" class="validation-pagination" aria-label="Páginas dos cadastros"><button class="btn outline" :disabled="pagina === 0 || salvando" @click="carregar(pagina - 1)">Anterior</button><span>Página {{ pagina + 1 }} de {{ totalPaginas }}</span><button class="btn outline" :disabled="pagina + 1 >= totalPaginas || salvando" @click="carregar(pagina + 1)">Próxima</button></nav>
      </template>
    </article>
  </section>
</template>

<style scoped>
.gestao label { display:grid; gap:7px; font-size:.85rem; }
.gestao :is(input, select, textarea) { padding:10px; border:1px solid var(--border); border-radius:6px; background:white; max-width:100%; }
.gestao .recurso { min-width:220px; }
.acoes { display:flex; gap:8px; flex-wrap:wrap; }
.editor { padding:20px; margin:20px 0; background:#f4f7fa; border-radius:8px; }
.campos { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px; margin:18px 0; }
.filtros { display:flex; flex-wrap:wrap; gap:12px; align-items:end; margin:16px 0; }
.validation-success { margin:12px 0; }
@media(max-width:700px) { .campos { grid-template-columns:1fr; }.editor { padding:12px; }.recurso { width:100%; } }
</style>
