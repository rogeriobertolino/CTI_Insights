import { defineStore } from 'pinia'
import * as XLSX from 'xlsx'
import { dataContrato } from '../utils/dataContrato.js'
import { parseFaturamento } from '../utils/faturamento.js'
import { enviarPlanilha } from '../services/analysis.js'
import { dataStore } from '../dataStore.js'

export const LIMITE_ARQUIVO_BYTES = 5 * 1024 * 1024
export const LIMITE_REGISTROS = 10000
export const LIMITE_COLUNAS = 100
export const COLUNAS_MODELO = Object.freeze(['codigo', 'nome', 'email', 'status', 'nivel'])
export const TIPOS_ERRO = Object.freeze({
  campo_vazio: 'Campos obrigatórios vazios',
  espacos: 'Espaços desnecessários',
  duplicado: 'Códigos duplicados',
  email_invalido: 'E-mails inválidos',
  fora_padrao: 'Dados fora do padrão',
  padronizacao: 'Textos para padronizar',
})

const OBRIGATORIAS = ['codigo', 'nome', 'email']
const OBRIGATORIAS_BASE_COMERCIAL = ['codigo', 'nome', 'segmento']
const ALIASES = new Map([
  ['codigo', 'codigo'], ['cod', 'codigo'], ['codigo_cliente', 'codigo'],
  ['cliente_codigo', 'codigo'], ['cod_cliente', 'codigo'], ['cnpj', 'codigo'],
  ['nome', 'nome'], ['nome_cliente', 'nome'], ['cliente_nome', 'nome'],
  ['cliente', 'nome'], ['razao_social', 'nome'], ['empresa', 'nome'],
  ['email', 'email'], ['e_mail', 'email'], ['email_cliente', 'email'],
  ['e_mail_cliente', 'email'], ['correio_eletronico', 'email'],
  ['faturamento', 'faturamento'], ['faturamento_mensal', 'faturamento'],
  ['faturamento_anual', 'faturamento'], ['receita', 'faturamento'],
  ['status', 'status'], ['situacao', 'status'],
  ['nivel', 'nivel'], ['nivel_cliente', 'nivel'], ['classificacao', 'nivel'],
])
const SEGMENTOS = new Map([
  ['IND.', 'Indústria'], ['INDUSTRIA', 'Indústria'], ['INDÚSTRIA', 'Indústria'],
  ['SAUDE', 'Saúde'], ['SAÚDE', 'Saúde'], ['VAREJO', 'Varejo'], ['COMERCIO', 'Comércio'], ['COMÉRCIO', 'Comércio'],
  ['SERVICOS', 'Serviços'], ['SERVIÇOS', 'Serviços'],
])
const STATUS = new Map([['ativo', 'Ativo'], ['inativo', 'Inativo'], ['pendente', 'Pendente']])
// Uma versão por store impede uma leitura antiga de substituir uma importação nova.
const versoes = new WeakMap()

function texto(valor) {
  return valor == null ? '' : String(valor)
}

function semEspacosExtras(valor) {
  return texto(valor).trim().replace(/\s+/gu, ' ')
}

function chaveCabecalho(valor) {
  const chave = texto(valor).trim().normalize('NFD').replace(/\p{M}/gu, '')
    .toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '')
  return ALIASES.get(chave) || chave
}

function chaveCodigo(valor) {
  return semEspacosExtras(valor).normalize('NFKC').toLocaleLowerCase('pt-BR')
}

function emailValido(valor) {
  if (valor.length > 254 || /\s/u.test(valor)) return false
  const partes = valor.split('@')
  if (partes.length !== 2) return false
  const [local, dominio] = partes
  if (!local || local.length > 64 || local.startsWith('.') || local.endsWith('.') || local.includes('..')) return false
  if (!/^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+$/.test(local)) return false
  const segmentos = dominio.split('.')
  return segmentos.length >= 2 && segmentos.every((parte) =>
    parte.length <= 63 && /^[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?$/.test(parte),
  ) && /^[A-Za-z]{2,63}$/.test(segmentos.at(-1))
}

function sugestaoNome(valor) {
  const conectores = new Set(['da', 'das', 'de', 'do', 'dos', 'e'])
  return valor.toLocaleLowerCase('pt-BR').replace(/\p{L}[\p{L}\p{M}]*/gu, (parte, posicao) =>
    posicao > 0 && conectores.has(parte) ? parte : parte[0].toLocaleUpperCase('pt-BR') + parte.slice(1),
  )
}

function lerAba(workbook) {
  const nomeAba = workbook.SheetNames[0]
  const aba = workbook.Sheets[nomeAba]
  if (!aba?.['!ref']) throw new Error('A primeira aba está vazia. Inclua os cabeçalhos e ao menos um registro.')
  const area = XLSX.utils.decode_range(aba['!fullref'] || aba['!ref'])
  // Também limita linhas/colunas vazias no intervalo declarado pelo arquivo.
  if (area.e.r > LIMITE_REGISTROS) throw new Error(`A área utilizada deve terminar até a linha ${LIMITE_REGISTROS + 1} (cabeçalho e até ${LIMITE_REGISTROS} registros).`)
  if (area.e.c >= LIMITE_COLUNAS) throw new Error(`A planilha deve usar somente as primeiras ${LIMITE_COLUNAS} colunas.`)
  const matriz = XLSX.utils.sheet_to_json(aba, { header: 1, raw: false, defval: '', blankrows: true, range: 0 })
  const linhaCabecalho = matriz.findIndex((linha) => linha.some((valor) => texto(valor).trim() !== ''))
  if (linhaCabecalho < 0) throw new Error('A primeira aba está vazia. Inclua os cabeçalhos e ao menos um registro.')
  const originais = matriz[linhaCabecalho]
  const cabecalhos = []
  for (let coluna = 0; coluna <= area.e.c; coluna += 1) {
    const chave = chaveCabecalho(originais[coluna])
    if (!chave) throw new Error(`O cabeçalho da coluna ${XLSX.utils.encode_col(coluna)} está vazio ou inválido. Informe um nome para cada coluna utilizada.`)
    if (cabecalhos.includes(chave)) throw new Error(`Cabeçalho duplicado: "${originais[coluna]}" corresponde à coluna "${chave}" já informada. Remova ou renomeie a coluna repetida.`)
    cabecalhos.push(chave)
  }
  const baseComercial = OBRIGATORIAS_BASE_COMERCIAL.every((coluna) => cabecalhos.includes(coluna))
  const contratos = ['valor_servico', 'status_contrato', 'data_inicio'].some(c => cabecalhos.includes(c))
  const obrigatorias = contratos ? ['codigo', 'nome', 'consultor', 'segmento', 'nivel', 'faturamento', 'servico', 'valor_servico', 'status_contrato', 'data_inicio'] : baseComercial ? OBRIGATORIAS_BASE_COMERCIAL : OBRIGATORIAS
  const ausentes = obrigatorias.filter((coluna) => !cabecalhos.includes(coluna))
  if (ausentes.length) throw new Error(`Colunas obrigatórias ausentes: ${ausentes.join(', ')}. Utilize o modelo disponível na tela de upload.`)
  const registros = []
  for (let indice = linhaCabecalho + 1; indice < matriz.length; indice += 1) {
    const valores = matriz[indice] || []
    // Apenas células realmente vazias são ignoradas. Uma linha com espaços é analisada.
    if (valores.every((valor) => texto(valor) === '')) continue
    const dados = Object.create(null)
    cabecalhos.forEach((campo, coluna) => {
      const cell = aba[XLSX.utils.encode_cell({ r: indice, c: coluna })]
      if (campo === 'data_inicio' && cell?.t === 'n' && XLSX.SSF.is_date(cell.z || '')) {
        const date = XLSX.SSF.parse_date_code(cell.v)
        dados[campo] = date ? `${String(date.y).padStart(4,'0')}-${String(date.m).padStart(2,'0')}-${String(date.d).padStart(2,'0')}` : ''
        return
      }
      dados[campo] = ['faturamento', 'valor_servico'].includes(campo) && cell?.t === 'n' ? cell.v : texto(valores[coluna])
    })
    registros.push({ linha: indice + 1, dados })
  }
  if (!registros.length) throw new Error('A primeira aba não contém registros. Adicione dados abaixo dos cabeçalhos.')
  const colunaReceita = cabecalhos.indexOf('faturamento')
  const tituloReceita = texto(originais[colunaReceita]).toLowerCase()
  const periodoFaturamento = tituloReceita.includes('mensal') ? 'mensal' : tituloReceita.includes('anual') ? 'anual' : 'informado'
  return { nomeAba, cabecalhos, registros, periodoFaturamento }
}

export const useUploadStore = defineStore('upload', {
  // STATE: dados temporários da importação, ocorrências e situação da análise.
  // Não há localStorage, envio ao servidor ou alteração do arquivo original.
  state: () => ({
    arquivo: null,
    nomeArquivo: '',
    nomeAba: '',
    registros: [],
    dadosOriginais: [],
    dadosTratados: [],
    erros: [],
    cabecalhos: [],
    carregando: false,
    erroArquivo: '',
    validadoEm: null,
    dataUpload: null,
    filtroSegmento: 'Todos',
    periodoFaturamento: 'informado',
    enviando: false,
    erroEnvio: '',
    analiseSalva: null,
  }),

  // GETTERS: estatísticas derivadas. Um registro pode ter várias ocorrências.
  getters: {
    segmentos: (state) => [...new Set(state.dadosTratados.map(c => c.segmento).filter(Boolean))].sort(),
    clientesFiltrados: (state) => state.dadosTratados.filter(c => state.filtroSegmento === 'Todos' || c.segmento === state.filtroSegmento),
    clientesValidos: (state) => state.dadosTratados.filter(c => !c.erros.length),
    totalFaturamento() { return this.clientesValidos.reduce((sum, c) => sum + Math.round((c.faturamento ?? 0) * 100), 0) / 100 },
    quantidadeComFaturamento() { return this.clientesValidos.filter(c => c.faturamento != null).length },
    faturamentoMedio() { return this.quantidadeComFaturamento ? Math.round(this.totalFaturamento / this.quantidadeComFaturamento * 100) / 100 : 0 },
    totalClientes: (state) => state.dadosTratados.length,
    clientesNivelA: (state) => state.dadosTratados.filter((cliente) => cliente.nivel_cliente === 'A').length,
    temDados: (state) => state.dadosTratados.length > 0,
    resumoPorSegmento: (state) => state.dadosTratados.reduce((resumo, cliente) => {
      const segmento = cliente.segmento || 'Não informado'
      resumo[segmento] = (resumo[segmento] || 0) + 1
      return resumo
    }, Object.create(null)),
    totalLinhas: (state) => state.registros.length,
    totalColunas: (state) => state.cabecalhos.length,
    totalValidas() { return this.totalValidos },
    linhasComErro() { return this.dadosTratados.filter((linha) => linha.erros.length > 0) },
    linhasValidas() { return this.dadosTratados.filter((linha) => linha.erros.length === 0) },
    erro() { return this.erroArquivo },
    totalRegistros: (state) => state.registros.length,
    totalErros: (state) => state.erros.length,
    totalComErro: (state) => new Set(state.erros.map((erro) => erro.linha)).size,
    totalValidos() { return this.totalRegistros - this.totalComErro },
    percentualValidos() { return this.totalRegistros ? (this.totalValidos / this.totalRegistros) * 100 : 0 },
    errosPorTipo: (state) => Object.entries(TIPOS_ERRO).map(([tipo, rotulo]) => ({
      tipo, rotulo, quantidade: state.erros.filter((erro) => erro.tipo === tipo).length,
    })),
    registrosValidos: (state) => {
      const linhasComErro = new Set(state.erros.map((erro) => erro.linha))
      return state.registros.filter((registro) => !linhasComErro.has(registro.linha))
    },
  },

  // ACTIONS: importação local, validação idempotente e limpeza da análise.
  actions: {
    selecionarArquivo(arquivo) {
      this.limpar()
      this.arquivo = arquivo
    },

    validarArquivo(arquivo = this.arquivo) {
      if (!arquivo || typeof arquivo.arrayBuffer !== 'function') throw new Error('Selecione uma planilha Excel ou CSV.')
      if (!/\.(xlsx|xls|csv)$/i.test(arquivo.name || '')) throw new Error('Formato não suportado. Selecione um arquivo .xlsx, .xls ou .csv.')
      if (!arquivo.size) throw new Error('O arquivo está vazio. Selecione uma planilha com dados.')
      if (arquivo.size > LIMITE_ARQUIVO_BYTES) throw new Error('O arquivo excede o limite de 5 MB.')
      return true
    },

    async processarPlanilha() {
      return this.importarArquivo(this.arquivo)
    },

    tratarLinha(linha) {
      const dados = Object.fromEntries(Object.entries(linha).map(([campo, valor]) => [campo, semEspacosExtras(valor)]))
      const segmento = texto(dados.segmento).toLocaleUpperCase('pt-BR')
      const nivel = texto(dados.nivel ?? dados.nivel_cliente).toLocaleUpperCase('pt-BR')
      return {
        ...dados,
        ...(Object.hasOwn(linha, 'faturamento') ? { faturamento: parseFaturamento(linha.faturamento) } : {}),
        ...(Object.hasOwn(linha, 'valor_servico') ? { valor_servico: parseFaturamento(linha.valor_servico) } : {}),
        ...(Object.hasOwn(linha, 'data_inicio') ? { data_inicio: dataContrato(linha.data_inicio) } : {}),
        ...(Object.hasOwn(linha, 'status_contrato') ? { status_contrato: texto(linha.status_contrato).toUpperCase() } : {}),
        codigo_cliente: dados.codigo ?? dados.codigo_cliente ?? '',
        nome_cliente: dados.nome ?? dados.nome_cliente ?? '',
        consultor: texto(dados.consultor),
        segmento: SEGMENTOS.get(segmento) || segmento,
        nivel,
        nivel_cliente: nivel,
      }
    },

    tratarDados() {
      const errosPorLinha = new Map()
      for (const erro of this.erros) {
        if (!errosPorLinha.has(erro.linha)) errosPorLinha.set(erro.linha, [])
        errosPorLinha.get(erro.linha).push(erro.descricao)
      }
      this.dadosOriginais = this.registros.map((registro) => ({ ...registro.dados }))
      this.dadosTratados = this.registros.map((registro) => ({
        ...this.tratarLinha(registro.dados),
        numero_linha: registro.linha,
        erros: errosPorLinha.get(registro.linha) || [],
      }))
    },

    limpar() {
      versoes.set(this, (versoes.get(this) || 0) + 1)
      this.$reset()
    },

    async importarArquivo(arquivo) {
      this.limpar()
      const versao = versoes.get(this)
      this.arquivo = arquivo
      this.carregando = true
      try {
        this.validarArquivo(arquivo)
        const buffer = await arquivo.arrayBuffer()
        if (versoes.get(this) !== versao) return false
        if (!buffer.byteLength || buffer.byteLength > LIMITE_ARQUIVO_BYTES) throw new Error('O arquivo deve conter dados e ter no máximo 5 MB.')
        const assinatura = new Uint8Array(buffer, 0, Math.min(4, buffer.byteLength))
        const extensao = arquivo.name.split('.').at(-1).toLowerCase()
        const zip = assinatura[0] === 0x50 && assinatura[1] === 0x4b && assinatura[2] === 0x03 && assinatura[3] === 0x04
        const ole = assinatura[0] === 0xd0 && assinatura[1] === 0xcf && assinatura[2] === 0x11 && assinatura[3] === 0xe0
        if ((extensao === 'xlsx' && !zip) || (extensao === 'xls' && !ole)) {
          throw new Error(`O conteúdo não é uma planilha .${extensao} válida. Abra o arquivo no Excel e salve novamente no formato selecionado.`)
        }
        let workbook
        try {
          if (extensao === 'csv') {
            const csv = new TextDecoder('utf-8', { fatal: true }).decode(buffer)
            if (csv.includes('\0')) throw new Error('CSV inválido')
            // raw evita perder zeros iniciais e reinterpretar datas em CSV.
            workbook = XLSX.read(csv, { type: 'string', raw: true, sheetRows: LIMITE_REGISTROS + 2 })
          } else {
            workbook = XLSX.read(buffer, { type: 'array', sheets: 0, cellNF: true, sheetRows: LIMITE_REGISTROS + 2 })
          }
        } catch {
          throw new Error('Não foi possível ler a planilha. O arquivo pode estar corrompido ou protegido por senha.')
        }
        const dados = lerAba(workbook)
        this.$patch({ ...dados, arquivo, nomeArquivo: arquivo.name, dataUpload: new Date() })
        this.validarDados()
        return true
      } catch (erro) {
        if (versoes.get(this) === versao) {
          this.$reset()
          this.arquivo = arquivo
          this.erroArquivo = erro instanceof Error ? erro.message : 'Não foi possível analisar a planilha.'
        }
        return false
      } finally {
        if (versoes.get(this) === versao) this.carregando = false
      }
    },

    prepararArquivoEnvio() {
      if (!this.cabecalhos.includes('segmento')) throw new Error('Para salvar a análise, inclua a coluna segmento na planilha.')
      const validos = this.clientesValidos
      if (!validos.length) throw new Error('Nenhum registro válido para enviar. Corrija a planilha e valide novamente.')
      const receita = this.periodoFaturamento === 'mensal' ? 'faturamento_mensal' : this.periodoFaturamento === 'anual' ? 'faturamento_anual' : 'faturamento'
      const campos = this.cabecalhos.map(c => ({ codigo: 'codigo_cliente', nome: 'nome_cliente', faturamento: receita })[c] || c)
      campos.push('linha_origem')
      const linhas = validos.map(c => [...this.cabecalhos.map(campo => c[campo] ?? ''), c.numero_linha])
      const workbook = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet([campos, ...linhas]), 'dados_importacao')
      const bytes = XLSX.write(workbook, { type: 'array', bookType: 'xlsx' })
      return new File([bytes], `${this.nomeArquivo.replace(/\.(xlsx|xls|csv)$/i, '')}-validado.xlsx`, { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    },

    async enviarDados() {
      if (this.enviando) return null
      this.erroEnvio = ''
      this.validarDados()
      const versao = versoes.get(this)
      this.enviando = true
      try {
        const resultado = await enviarPlanilha(this.prepararArquivoEnvio())
        if (versoes.get(this) !== versao) return null
        this.analiseSalva = resultado
        dataStore.analysis = resultado
        dataStore.error = ''
        return resultado
      } catch (erro) {
        if (versoes.get(this) === versao) this.erroEnvio = erro.message
        return null
      } finally {
        if (versoes.get(this) === versao) this.enviando = false
      }
    },

    async lerArquivo(arquivo) {
      return this.importarArquivo(arquivo)
    },

    validarDados() {
      const erros = []
      const porCodigo = new Map()
      const contratos = ['valor_servico', 'status_contrato', 'data_inicio'].some(c => this.cabecalhos.includes(c))
      const camposObrigatorios = contratos ? ['codigo', 'nome', 'consultor', 'segmento', 'nivel', 'faturamento', 'servico', 'valor_servico', 'status_contrato', 'data_inicio'] : this.cabecalhos.includes('segmento')
        ? OBRIGATORIAS_BASE_COMERCIAL
        : OBRIGATORIAS
      const registrar = (registro, campo, tipo, descricao, sugestao) => {
        const erro = { linha: registro.linha, campo, tipo, descricao, valor: texto(registro.dados[campo]) }
        if (sugestao !== undefined) erro.sugestao = sugestao
        erros.push(erro)
      }
      for (const registro of this.registros) {
        const { dados } = registro
        for (const campo of camposObrigatorios) {
          if (!texto(dados[campo]).trim()) registrar(registro, campo, 'campo_vazio', 'Campo obrigatório não preenchido.')
        }
        for (const [campo, original] of Object.entries(dados)) {
          if (['faturamento', 'valor_servico'].includes(campo) && typeof original === 'number') continue
          const valor = texto(original)
          const limpo = semEspacosExtras(valor)
          if (!contratos && valor !== limpo) registrar(registro, campo, 'espacos', 'Remova espaços no início/fim, repetições, tabulações ou quebras de linha.', limpo)
        }
        if (contratos) {
          const valor = parseFaturamento(dados.valor_servico)
          if (valor === null || valor < 0 || valor >= 1e13 || Math.abs(valor * 100 - Math.round(valor * 100)) > 0.00001) registrar(registro, 'valor_servico', 'fora_padrao', 'Informe um valor de serviço não negativo, com até duas casas decimais.')
          const statusContrato = semEspacosExtras(dados.status_contrato).toUpperCase()
          if (!['ATIVO', 'PENDENTE', 'CANCELADO'].includes(statusContrato)) registrar(registro, 'status_contrato', 'fora_padrao', 'Use ATIVO, PENDENTE ou CANCELADO.')
          if (!dataContrato(dados.data_inicio)) registrar(registro, 'data_inicio', 'fora_padrao', 'Informe uma data válida no formato AAAA-MM-DD ou DD/MM/AAAA.')
        }
        if (this.cabecalhos.includes('faturamento')) {
          const receita = parseFaturamento(dados.faturamento)
          if (receita === null || receita < 0 || receita >= 1e13 || Math.abs(receita * 100 - Math.round(receita * 100)) > 0.00001) registrar(registro, 'faturamento', 'fora_padrao', 'Informe um faturamento numérico não negativo.')
        }
        const codigo = texto(dados.codigo).trim()
        if (codigo) {
          if (!/^[A-Za-z0-9][A-Za-z0-9._/-]*$/.test(codigo)) {
            registrar(registro, 'codigo', 'fora_padrao', 'Use letras sem acentos e números; ponto, hífen, barra e sublinhado são permitidos após o primeiro caractere.')
          }
          const chave = chaveCodigo(codigo)
          if (!porCodigo.has(chave)) porCodigo.set(chave, [])
          porCodigo.get(chave).push(registro)
        }
        const email = texto(dados.email).trim()
        if (email) {
          if (!emailValido(email)) registrar(registro, 'email', 'email_invalido', 'Informe um e-mail válido, como nome@dominio.com, sem espaços ou pontos consecutivos.')
          else if (email !== email.toLowerCase()) registrar(registro, 'email', 'padronizacao', 'Revise o e-mail conforme a convenção da planilha: letras minúsculas. A sugestão não altera os dados.', email.toLowerCase())
        }
        const nome = semEspacosExtras(dados.nome)
        if (nome) {
          // Nomes de empresas podem incluir números e símbolos da razão social.
          const nomeValido = this.cabecalhos.includes('segmento')
            ? /[\p{L}\p{N}]/u.test(nome)
            : /^[\p{L}\p{M} .’'\-]+$/u.test(nome) && /\p{L}/u.test(nome)
          if (!nomeValido) {
            registrar(registro, 'nome', 'fora_padrao', 'O nome deve conter letras; espaços, apóstrofos, pontos e hífens são permitidos.')
          }
          const letras = nome.match(/\p{L}/gu) || []
          if (!contratos && letras.length > 1 && (nome === nome.toLocaleUpperCase('pt-BR') || nome === nome.toLocaleLowerCase('pt-BR'))) {
            registrar(registro, 'nome', 'padronizacao', 'Revise o nome escrito inteiramente em maiúsculas ou minúsculas. A sugestão não altera os dados.', sugestaoNome(nome))
          }
        }
        const status = semEspacosExtras(dados.status)
        if (status) {
          const canonico = STATUS.get(status.toLocaleLowerCase('pt-BR'))
          if (!canonico) registrar(registro, 'status', 'fora_padrao', 'Os valores permitidos são Ativo, Inativo ou Pendente.')
          else if (status !== canonico) registrar(registro, 'status', 'padronizacao', 'Padronize o status como Ativo, Inativo ou Pendente.', canonico)
        }
        const nivel = semEspacosExtras(dados.nivel)
        if (nivel) {
          const canonico = nivel.toLocaleUpperCase('pt-BR')
          if (!['A', 'B', 'C'].includes(canonico)) registrar(registro, 'nivel', 'fora_padrao', 'Os valores permitidos para nível são A, B ou C.')
          else if (!contratos && nivel !== canonico) registrar(registro, 'nivel', 'padronizacao', 'Padronize o nível com letra maiúscula: A, B ou C.', canonico)
        }
      }
      for (const grupo of porCodigo.values()) {
        if (grupo.length < 2) continue
        const linhas = grupo.slice(0, 10).map((registro) => registro.linha).join(', ')
          + (grupo.length > 10 ? ` e mais ${grupo.length - 10} linhas` : '')
        for (const registro of grupo) registrar(registro, 'codigo', 'duplicado', `Código repetido nas linhas ${linhas}. A comparação ignora maiúsculas/minúsculas e espaços extras.`)
      }
      this.erros = erros.sort((a, b) => a.linha - b.linha)
      this.tratarDados()
      this.validadoEm = this.registros.length ? new Date().toISOString() : null
      return this.erros
    },
  },
})
