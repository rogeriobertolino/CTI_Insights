import { reactive } from 'vue'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1'

export const dataStore = reactive({
  analysis: null,
  loading: false,
  error: '',
})

async function request(path, options) {
  const response = await fetch(`${API_URL}${path}`, options)
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message || `Falha na API (${response.status}).`)
  return body
}

export async function uploadWorkbook(file) {
  dataStore.loading = true
  dataStore.error = ''
  try {
    const form = new FormData()
    form.append('file', file)
    dataStore.analysis = await request('/imports', { method: 'POST', body: form })
    return dataStore.analysis
  } catch (error) {
    dataStore.error = error.message
    throw error
  } finally {
    dataStore.loading = false
  }
}

export async function loadLatestAnalysis() {
  if (dataStore.analysis) return dataStore.analysis
  dataStore.loading = true
  try {
    dataStore.analysis = await request('/dashboard/latest')
  } catch (error) {
    if (!/Nenhuma análise/.test(error.message)) dataStore.error = error.message
  } finally {
    dataStore.loading = false
  }
  return dataStore.analysis
}

export function clearData() {
  dataStore.analysis = null
  dataStore.error = ''
}
