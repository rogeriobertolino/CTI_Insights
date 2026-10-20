export function dataContrato(value) {
  const text = String(value ?? '').trim()
  let iso = text
  const br = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(text)
  if (br) iso = `${br[3]}-${br[2]}-${br[1]}`
  if (!/^\d{4}-\d{2}-\d{2}$/.test(iso)) return null
  const parsed = new Date(iso + 'T00:00:00Z')
  return Number.isNaN(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== iso ? null : iso
}
