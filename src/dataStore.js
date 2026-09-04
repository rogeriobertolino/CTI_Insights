import { reactive } from 'vue'

export const dataStore = reactive({
  fileName: '',
  sheetName: '',
  headers: [],
  rows: [],
  analysis: null,
  uploadedAt: null,
})

const clean = value => String(value ?? '').trim()
const key = value => clean(value).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '_')
const cellValue = cell => {
  const value = cell.value
  if (value && typeof value === 'object') return clean(value.text ?? value.result ?? value.richText?.map(item => item.text).join('') ?? value.hyperlink ?? '')
  return clean(value)
}
const findColumn = (headers, aliases) => {
  for (const alias of aliases) {
    const index = headers.findIndex(header => key(header) === alias || key(header).includes(alias))
    if (index >= 0) return index
  }
  return -1
}
const countValues = (rows, index, split = false) => {
  if (index < 0) return []
  const counts = new Map()
  rows.forEach(row => {
    const values = split ? clean(row[index]).split(/[,;|/]+/) : [clean(row[index])]
    values.map(value => value.trim()).filter(Boolean).forEach(value => counts.set(value, (counts.get(value) || 0) + 1))
  })
  return [...counts].map(([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value)
}

export async function analyzeWorkbook(file) {
  const { default: ExcelJS } = await import('exceljs')
  const workbook = new ExcelJS.Workbook()
  await workbook.xlsx.load(await file.arrayBuffer())
  const worksheet = workbook.worksheets[0]
  if (!worksheet) throw new Error('A planilha não possui abas legíveis.')

  const headers = []
  worksheet.getRow(1).eachCell({ includeEmpty: true }, (cell, column) => { headers[column - 1] = cellValue(cell) || `Coluna ${column}` })
  const rows = []
  worksheet.eachRow({ includeEmpty: false }, (row, number) => {
    if (number === 1) return
    const values = headers.map((_, index) => cellValue(row.getCell(index + 1)))
    if (values.some(Boolean)) rows.push(values)
  })

  const clientCode = findColumn(headers, ['cliente_codigo', 'codigo_cliente', 'cod_cliente', 'cnpj'])
  const clientName = findColumn(headers, ['razao_social', 'nome_cliente', 'cliente', 'empresa'])
  const segment = findColumn(headers, ['segmento', 'setor'])
  const status = findColumn(headers, ['status', 'situacao'])
  const service = findColumn(headers, ['servico', 'produto', 'solucao'])
  const consultant = findColumn(headers, ['consultor', 'responsavel', 'vendedor'])
  const level = findColumn(headers, ['nivel', 'classificacao'])
  const clientIndex = clientCode >= 0 ? clientCode : clientName
  const clientKeys = rows.map(row => key(row[clientIndex])).filter(Boolean)
  const uniqueClients = new Set(clientKeys)
  const emptyCells = rows.reduce((total, row) => total + row.filter(value => !clean(value)).length, 0)
  const totalCells = Math.max(rows.length * headers.length, 1)
  const duplicates = Math.max(clientKeys.length - uniqueClients.size, 0)
  const missingKey = clientIndex < 0 ? rows.length : rows.filter(row => !clean(row[clientIndex])).length
  const completeness = Math.max(0, 100 - (emptyCells / totalCells) * 100)
  const quality = Math.max(0, completeness - (duplicates / Math.max(rows.length, 1)) * 20 - (missingKey / Math.max(rows.length, 1)) * 30)

  dataStore.fileName = file.name
  dataStore.sheetName = worksheet.name
  dataStore.headers = headers
  dataStore.rows = rows
  dataStore.uploadedAt = new Date()
  dataStore.analysis = {
    totalRows: rows.length,
    totalClients: clientIndex >= 0 ? uniqueClients.size + missingKey : rows.length,
    activeClients: status >= 0 ? rows.filter(row => /ativo/i.test(row[status]) && !/inativo/i.test(row[status])).length : null,
    completeness,
    quality,
    duplicates,
    missingKey,
    emptyCells,
    segments: countValues(rows, segment),
    statuses: countValues(rows, status),
    services: countValues(rows, service, true),
    consultants: countValues(rows, consultant),
    levels: countValues(rows, level),
    columns: { clientCode, clientName, segment, status, service, consultant, level },
  }
  return dataStore.analysis
}

export function clearData() {
  dataStore.fileName = ''
  dataStore.sheetName = ''
  dataStore.headers = []
  dataStore.rows = []
  dataStore.analysis = null
  dataStore.uploadedAt = null
}
