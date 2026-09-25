import { reactive } from 'vue'
import { requestApi } from './api.js'

const API_URL = import.meta.env.VITE_API_URL || '/api/v1'

export const dataStore = reactive({
  analysis: null,
  loading: false,
  error: '',
})

function request(path, options) {
  return requestApi(`${API_URL.replace(/\/$/, '')}${path}`, options)
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
  dataStore.error = ''
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
