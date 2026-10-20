// Valores numéricos do Excel mantêm o tipo; textos aceitam moeda brasileira e decimal internacional.
export function parseFaturamento(value) {
  if (typeof value === 'number') return Number.isFinite(value) ? value : null
  let text = String(value ?? '').trim().replace(/^R\$\s*/, '').replace(/\s/g, '')
  if (!text) return null
  if (text.includes(',') && text.includes('.')) {
    const decimal = text.lastIndexOf(',') > text.lastIndexOf('.') ? ',' : '.'
    const group = decimal === ',' ? '.' : ','
    const [integer, fraction, extra] = text.split(decimal)
    if (extra !== undefined || !/^\d{1,2}$/.test(fraction) || !new RegExp('^[+-]?\\d{1,3}(?:\\' + group + '\\d{3})+$').test(integer)) return null
    text = integer.split(group).join('') + '.' + fraction
  } else if (text.includes(',')) {
    if (!/^[+-]?\d+,\d{1,2}$/.test(text)) return null
    text = text.replace(',', '.')
  }
  if (!/^[+-]?\d+(?:\.\d{1,2})?$/.test(text)) return null
  const number = Number(text)
  return Number.isFinite(number) ? number : null
}
