export function csvSeguro(rows) {
  const headers = [...new Set(rows.flatMap(row => Object.keys(row)))]
  const cell = value => {
    let text = value == null ? '' : typeof value === 'object' ? JSON.stringify(value) : String(value)
    if (typeof value === 'string' && /^[\s]*[=+@-]/.test(text)) text = "'" + text
    return `"${text.replaceAll('"', '""')}"`
  }
  return '\ufeff' + [headers, ...rows.map(row => headers.map(h => row[h]))].map(row => row.map(cell).join(';')).join('\r\n')
}
export function baixarRelatorio(nome, rows) {
  const url = URL.createObjectURL(new Blob([csvSeguro(rows)], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a'); link.href = url; link.download = nome; link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
