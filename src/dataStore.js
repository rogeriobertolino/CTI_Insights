import { reactive } from 'vue'
import { enviarPlanilha, buscarUltimaAnalise } from './services/analysis.js'


export const dataStore = reactive({
  analysis: null,
  loading: false,
  error: '',
})


export async function uploadWorkbook(file) {
  dataStore.loading = true
  dataStore.error = ''
  try {
    dataStore.analysis = await enviarPlanilha(file)
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
    dataStore.analysis = await buscarUltimaAnalise()
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
