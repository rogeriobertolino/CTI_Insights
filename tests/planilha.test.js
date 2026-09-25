import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createPinia, setActivePinia } from 'pinia'
import * as XLSX from 'xlsx'
import {
  usePlanilhaStore, COLUNAS_MODELO, TIPOS_ERRO, LIMITE_ARQUIVO_BYTES,
} from '../src/stores/planilha.js'

function storeNova() {
  setActivePinia(createPinia())
  return usePlanilhaStore()
}

function arquivo(linhas, { nome = 'clientes.xlsx', segundaAba, ajustarAba } = {}) {
  const workbook = XLSX.utils.book_new()
  const aba = XLSX.utils.aoa_to_sheet(linhas)
  if (ajustarAba) ajustarAba(aba)
  XLSX.utils.book_append_sheet(workbook, aba, 'Clientes')
  if (segundaAba) XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet(segundaAba), 'Ignorada')
  const buffer = XLSX.write(workbook, { type: 'array', bookType: 'xlsx' })
  return { name: nome, size: buffer.byteLength, arrayBuffer: async () => buffer }
}

const cabecalho = [...COLUNAS_MODELO]
const registroValido = ['C001', 'Ana Silva', 'ana@example.com', 'Ativo', 'A']

test('XLSX real: calcula registros, ocorrências por tipo e marca todos os códigos duplicados', async () => {
  const store = storeNova()
  const sucesso = await store.importarArquivo(arquivo([
    cabecalho,
    registroValido,
    ['C002', '  JOÃO  SILVA ', 'joao@invalido', 'ativo', 'a'],
    [' c001 ', '', '', 'Bloqueado', 'D'],
    ['C003', 'Maria Souza', 'maria@example.com', 'Pendente', 'B'],
  ]))
  assert.equal(sucesso, true)
  assert.equal(store.nomeArquivo, 'clientes.xlsx')
  assert.equal(store.nomeAba, 'Clientes')
  assert.equal(store.totalRegistros, 4)
  assert.equal(store.totalValidos, 1)
  assert.equal(store.totalComErro, 3)
  assert.equal(store.totalErros, 12)
  assert.equal(store.percentualValidos, 25)
  assert.deepEqual(store.registrosValidos.map((registro) => registro.linha), [5])
  assert.deepEqual(Object.fromEntries(store.errosPorTipo.map(({ tipo, quantidade }) => [tipo, quantidade])), {
    campo_vazio: 2, espacos: 2, duplicado: 2, email_invalido: 1, fora_padrao: 2, padronizacao: 3,
  })
  assert.deepEqual(store.erros.filter((erro) => erro.tipo === 'duplicado').map((erro) => erro.linha), [2, 4])
  assert.equal(store.registros[1].dados.nome, '  JOÃO  SILVA ')
  assert.equal(store.erros.find((erro) => erro.linha === 3 && erro.campo === 'nome' && erro.tipo === 'padronizacao').sugestao, 'João Silva')
  assert.ok(store.validadoEm)
  assert.equal(store.carregando, false)
  const erros = JSON.stringify(store.erros)
  store.validarDados()
  assert.equal(JSON.stringify(store.erros), erros, 'validar novamente não acumula erros nem modifica valores')
})

test('aliases, cabeçalho após linha vazia e linhas vazias preservam a numeração original', async () => {
  const store = storeNova()
  assert.equal(await store.importarArquivo(arquivo([
    [],
    ['Código Cliente', 'Razão Social', 'E-mail'],
    ['001', 'Ana Silva', 'ana@example.com'],
    [],
    ['002', 'Bia Costa', 'inválido'],
  ], { segundaAba: [['invalida']] })), true)
  assert.deepEqual([...store.cabecalhos], ['codigo', 'nome', 'email'])
  assert.deepEqual(store.registros.map((registro) => registro.linha), [3, 5])
  assert.equal(store.erros[0].linha, 5)
  assert.equal(store.erros[0].tipo, 'email_invalido')
  assert.equal(store.totalValidos, 1)
})

test('colunas opcionais podem faltar ou ficar vazias; getters contam tipos sem ocorrências', async () => {
  const store = storeNova()
  assert.equal(await store.importarArquivo(arquivo([cabecalho, ['C001', 'Ana Silva', 'ana@example.com']])), true)
  assert.equal(store.totalValidos, 1)
  assert.equal(store.totalErros, 0)
  assert.equal(store.errosPorTipo.length, Object.keys(TIPOS_ERRO).length)
  assert.ok(store.errosPorTipo.every((item) => item.quantidade === 0))
})

test('aceita base comercial sem e-mail quando codigo, nome e segmento existem', async () => {
  const store = storeNova()
  assert.equal(await store.importarArquivo(arquivo([
    ['codigo_cliente', 'nome_cliente', 'segmento', 'status', 'consultor'],
    ['T001', 'Empresa Teste', 'Comercio', 'Ativo', 'João'],
  ])), true)
  assert.equal(store.totalRegistros, 1)
  assert.equal(store.totalErros, 0)
  assert.equal(store.dadosTratados[0].codigo_cliente, 'T001')
})

test('campos compostos só de espaços são vazios e linha com espaços não é descartada', async () => {
  const store = storeNova()
  assert.equal(await store.importarArquivo(arquivo([cabecalho, [' ', '\t', '\n']])), true)
  assert.equal(store.totalRegistros, 1)
  assert.equal(store.erros.filter((erro) => erro.tipo === 'campo_vazio').length, 3)
  assert.equal(store.erros.filter((erro) => erro.tipo === 'espacos').length, 3)
  assert.equal(store.totalComErro, 1)
})

test('rejeita cabeçalhos ausentes, duplicados por alias e vazios com mensagem acionável', async () => {
  const store = storeNova()
  for (const [colunas, mensagem] of [
    [['codigo', 'nome'], /obrigatórias ausentes: email/],
    [['codigo', 'Código Cliente', 'nome', 'email'], /Cabeçalho duplicado/],
    [['codigo', '', 'email'], /coluna B está vazio/],
  ]) {
    assert.equal(await store.importarArquivo(arquivo([colunas, registroValido.slice(0, colunas.length)])), false)
    assert.match(store.erroArquivo, mensagem)
    assert.equal(store.totalRegistros, 0)
    assert.equal(store.validadoEm, null)
  }
})

test('planilha vazia ou só com cabeçalho não gera relatório de sucesso', async () => {
  const store = storeNova()
  assert.equal(await store.importarArquivo(arquivo([])), false)
  assert.match(store.erroArquivo, /vazia/)
  assert.equal(await store.importarArquivo(arquivo([cabecalho])), false)
  assert.match(store.erroArquivo, /não contém registros/)
})

test('rejeita extensão incorreta, arquivo vazio, tamanho excessivo e conteúdo inválido', async () => {
  const store = storeNova()
  const bytes = new TextEncoder().encode('codigo,nome,email\n1,Ana,ana@example.com').buffer
  for (const [entrada, mensagem] of [
    [null, /Selecione/],
    [{ name: 'dados.csv', size: bytes.byteLength, arrayBuffer: async () => bytes }, /Formato não suportado/],
    [{ name: 'vazio.xlsx', size: 0, arrayBuffer: async () => new ArrayBuffer(0) }, /vazio/],
    [{ name: 'grande.xlsx', size: LIMITE_ARQUIVO_BYTES + 1, arrayBuffer: async () => bytes }, /5 MB/],
    [{ name: 'renomeado.xlsx', size: bytes.byteLength, arrayBuffer: async () => bytes }, /conteúdo não é/],
    [{ name: 'corrompido.xlsx', size: 4, arrayBuffer: async () => new Uint8Array([0x50, 0x4b, 3, 4]).buffer }, /corrompido/],
  ]) {
    assert.equal(await store.importarArquivo(entrada), false)
    assert.match(store.erroArquivo, mensagem)
    assert.equal(store.carregando, false)
  }
})

test('limites de linhas e colunas impedem processar área excessiva', async () => {
  const store = storeNova()
  assert.equal(await store.importarArquivo(arquivo([cabecalho, registroValido], {
    ajustarAba: (aba) => { aba['!ref'] = 'A1:E10002' },
  })), false)
  assert.match(store.erroArquivo, /linha 10001/)
  const muitasColunas = Array.from({ length: 101 }, (_, indice) => `coluna${indice}`)
  assert.equal(await store.importarArquivo(arquivo([muitasColunas])), false)
  assert.match(store.erroArquivo, /100 colunas/)
})

test('limpar e falha de reimportação eliminam resultados anteriores', async () => {
  const store = storeNova()
  await store.importarArquivo(arquivo([cabecalho, registroValido]))
  assert.equal(store.totalRegistros, 1)
  await store.importarArquivo(arquivo([['nome'], ['Ana Silva']]))
  assert.equal(store.nomeArquivo, '')
  assert.equal(store.nomeAba, '')
  assert.equal(store.totalRegistros, 0)
  assert.equal(store.totalErros, 0)
  assert.equal(store.validadoEm, null)
  assert.ok(store.erroArquivo)
  await store.importarArquivo(arquivo([cabecalho, registroValido], { nome: 'novo.xlsx' }))
  assert.equal(store.nomeArquivo, 'novo.xlsx')
  assert.equal(store.erroArquivo, '')
  store.limpar()
  assert.equal(store.totalRegistros, 0)
  assert.equal(store.nomeArquivo, '')
  assert.equal(store.erroArquivo, '')
  assert.equal(store.percentualValidos, 0)
})

test('importação lenta anterior não substitui arquivo mais recente e limpar cancela leitura', async () => {
  const store = storeNova()
  const anterior = arquivo([cabecalho, registroValido], { nome: 'anterior.xlsx' })
  let liberar
  const buffer = await anterior.arrayBuffer()
  anterior.arrayBuffer = () => new Promise((resolve) => { liberar = resolve })
  const pendente = store.importarArquivo(anterior)
  assert.equal(store.carregando, true)
  assert.equal(await store.importarArquivo(arquivo([cabecalho, registroValido], { nome: 'atual.xlsx' })), true)
  liberar(buffer)
  assert.equal(await pendente, false)
  assert.equal(store.nomeArquivo, 'atual.xlsx')
  const cancelada = store.importarArquivo(anterior)
  store.limpar()
  liberar(buffer)
  assert.equal(await cancelada, false)
  assert.equal(store.nomeArquivo, '')
  assert.equal(store.totalRegistros, 0)
})

test('e-mails malformados e códigos/nomes fora do padrão geram ocorrências específicas', async () => {
  const store = storeNova()
  await store.importarArquivo(arquivo([
    cabecalho,
    ['-001', 'Ana 123', 'ana..silva@example.com'],
    ['C002', 'Bia Costa', 'bia@-dominio.com'],
    ['C003', 'Lia Souza', '.lia@example.com'],
    ['C004', 'José D’Ávila', 'jose+cti@sub.example.com'],
  ]))
  assert.equal(store.erros.filter((erro) => erro.tipo === 'email_invalido').length, 3)
  assert.equal(store.erros.filter((erro) => erro.tipo === 'fora_padrao').length, 2)
  assert.deepEqual(store.registrosValidos.map((registro) => registro.linha), [5])
})

test('cabeçalhos adicionais não alteram protótipos e valores formatados mantêm zeros iniciais', async () => {
  const store = storeNova()
  await store.importarArquivo(arquivo([
    ['codigo', 'nome', 'email', '__proto__', 'constructor'],
    [7, 'Ana Silva', 'ana@example.com', 'valor seguro', 'outro valor'],
  ], { ajustarAba: (aba) => { aba.A2.z = '0000' } }))
  assert.equal(store.registros[0].dados.codigo, '0007')
  assert.equal(store.registros[0].dados.proto, 'valor seguro')
  assert.equal(store.registros[0].dados.constructor, 'outro valor')
  assert.equal(Object.getPrototypeOf(store.registros[0].dados), null)
  assert.equal(store.totalValidos, 1)
})

test('e-mail válido em maiúsculas recebe sugestão de padronização sem alterar o original', async () => {
  const store = storeNova()
  assert.equal(await store.importarArquivo(arquivo([
    cabecalho,
    ['C001', 'Diego Silva', 'DIEGO@EXAMPLE.COM'],
    ['C002', 'Ana Silva', 'ANA@INVALIDO'],
  ])), true)
  const ocorrencia = store.erros.find((erro) => erro.linha === 2)
  assert.equal(ocorrencia.tipo, 'padronizacao')
  assert.equal(ocorrencia.campo, 'email')
  assert.equal(ocorrencia.sugestao, 'diego@example.com')
  assert.equal(store.registros[0].dados.email, 'DIEGO@EXAMPLE.COM')
  assert.equal(store.erros.filter((erro) => erro.linha === 3).length, 1)
  assert.equal(store.erros.find((erro) => erro.linha === 3).tipo, 'email_invalido')
  assert.equal(store.totalValidos, 0)
})
