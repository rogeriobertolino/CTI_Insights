import { dataContrato } from './dataContrato.js'
import { parseFaturamento } from './faturamento.js'

export const CAMPOS_ANUAIS = ['codigo', 'nome', 'consultor', 'segmento', 'nivel', 'faturamento', 'servicos_contratados', 'data_contratacao']
export const ehAnual = campos => campos.includes('servicos_contratados') || campos.includes('data_contratacao')
const chave = value => String(value ?? '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().replace(/[^a-z0-9]/g, '')
const segmentos = { ind: 'Indústria', industria: 'Indústria', comercio: 'Comércio', servico: 'Serviços', servicos: 'Serviços', saude: 'Saúde', educacao: 'Educação', tecnologia: 'Tecnologia' }
const limpo = v => String(v ?? '').trim().replace(/\s+/g, ' ')
const titulo = v => limpo(v).toLocaleLowerCase('pt-BR').replace(/\p{L}+/gu, (p, i) => i && ['de','da','do','dos','das','e'].includes(p) ? p : p[0].toUpperCase() + p.slice(1))
const moeda = v => parseFaturamento(typeof v === 'string' && /^\d{1,3}(\.\d{3})+$/.test(v.trim().replace(/^R\$\s*/, '')) ? v.trim().replace(/^R\$\s*/, '').replaceAll('.', '') : v)
export function tratarAnual(row) {
  const dados = Object.fromEntries(Object.entries(row).map(([k,v]) => [k, limpo(v)]))
  return { ...dados, codigo: limpo(dados.codigo).toUpperCase(), codigo_cliente: limpo(dados.codigo).toUpperCase(), nome_cliente: dados.nome,
    consultor: titulo(dados.consultor), segmento: segmentos[chave(dados.segmento)] || dados.segmento,
    nivel: limpo(dados.nivel).toUpperCase(), nivel_cliente: limpo(dados.nivel).toUpperCase(), faturamento: moeda(row.faturamento),
    data_contratacao: dataContrato(row.data_contratacao), cidade: titulo(dados.cidade), uf: limpo(dados.uf).toUpperCase(),
    servicos_contratados: [...new Set(limpo(dados.servicos_contratados).split(';').map(titulo).filter(Boolean))].join(';') }
}
export function validarAnual(registros) {
  const erros = [], avisos = [], vistos = new Set()
  for (const registro of registros) {
    const row = registro.dados, dados = tratarAnual(row)
    const add = (campo, descricao, tipo = 'fora_padrao') => erros.push({ linha: registro.linha, campo, tipo, descricao, valor: String(row[campo] ?? '') })
    for (const campo of CAMPOS_ANUAIS) if (!limpo(row[campo])) add(campo, 'Campo obrigatório não preenchido.', 'campo_vazio')
    for (const [campo, limite] of Object.entries({codigo:60,nome:180,consultor:120,segmento:80,servicos_contratados:400,cidade:120,uf:2}))
      if (limpo(row[campo]).length > limite) add(campo, `Máximo de ${limite} caracteres.`)
    if (!/^[A-Za-z0-9_-]{1,60}$/.test(dados.codigo)) add('codigo', 'Use letras, números, hífen ou sublinhado.')
    if (vistos.has(dados.codigo)) add('codigo', 'Código duplicado: a primeira ocorrência foi mantida.', 'duplicado')
    vistos.add(dados.codigo)
    if (!segmentos[chave(row.segmento)]) add('segmento', 'Use Indústria, Comércio, Serviços, Saúde, Educação ou Tecnologia.')
    if (!['A','B','C'].includes(dados.nivel)) add('nivel', 'Use A, B ou C.')
    if (dados.faturamento == null || dados.faturamento < 0 || dados.faturamento >= 1e12 || Math.abs(dados.faturamento * 100 - Math.round(dados.faturamento * 100)) > 0.00001) add('faturamento', 'Informe faturamento anual não negativo com até duas casas decimais.')
    if (!dados.servicos_contratados) add('servicos_contratados', 'Informe ao menos um serviço.')
    if (!dados.data_contratacao || Number(dados.data_contratacao.slice(0,4)) < 1990) add('data_contratacao', 'Informe uma data válida a partir de 1990.')
    if (dados.uf && !/^[A-Z]{2}$/.test(dados.uf)) add('uf', 'Informe a UF com duas letras.')
    for (const campo of Object.keys(row)) if (String(row[campo] ?? '') !== String(dados[campo] ?? '')) avisos.push({linha:registro.linha,campo,descricao:'Valor padronizado; original preservado.',valor:row[campo],sugestao:dados[campo]})
    if (dados.faturamento === 0) avisos.push({linha:registro.linha,campo:'faturamento',descricao:'Faturamento zerado.'})
    if (dados.data_contratacao > new Date().toISOString().slice(0,10)) avisos.push({linha:registro.linha,campo:'data_contratacao',descricao:'Data de contratação no futuro.'})
  }
  return {erros, avisos}
}
