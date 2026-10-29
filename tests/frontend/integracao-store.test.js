import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createPinia, setActivePinia } from 'pinia'
import * as XLSX from 'xlsx'
import { useUploadStore } from '../../src/stores/uploadStore.js'
import { analysisClient } from '../../src/services/analysis.js'
import { salvarToken, limparToken } from '../../src/services/token.js'
import { dataStore } from '../../src/stores/dataStore.js'
function arquivo() {
  const wb=XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb,XLSX.utils.aoa_to_sheet([
    ['codigo_cliente','cliente','segmento','faturamento'],
    ['1','Empresa 001','Serviços',100], ['2','Empresa 002','Comércio',200], ['3','Empresa 003','Comércio',-1],
  ]),'Clientes')
  const bytes=XLSX.write(wb,{type:'array',bookType:'xlsx'})
  return {name:'base.xlsx',size:bytes.byteLength,arrayBuffer:async()=>bytes}
}
function adapters(t, handler) {
  const original=analysisClient.defaults.adapter
  salvarToken('jwt-test')
  t.after(()=>{analysisClient.defaults.adapter=original;limparToken();dataStore.analysis=null})
  analysisClient.defaults.adapter=handler
}
test('action envia todos os segmentos válidos via Axios com Bearer JWT e atualiza o dashboard', async t=>{
  setActivePinia(createPinia());const store=useUploadStore();await store.importarArquivo(arquivo());store.filtroSegmento='Serviços'
  let sent
  adapters(t,async config=>{
    assert.equal(config.headers.get('Authorization'),'Bearer jwt-test')
    sent=config.data.get('file')
    return {data:{batchId:'test',totalRows:2},status:200,headers:{'content-type':'application/json'},config}
  })
  assert.equal((await store.enviarDados()).totalRows,2)
  const wb=XLSX.read(await sent.arrayBuffer());const rows=XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]])
  assert.deepEqual(rows.map(row=>row.codigo_cliente),['1','2'])
  assert.equal(dataStore.analysis.batchId,'test')
  assert.equal(store.enviando,false)
})
test('falha de envio mantém dados locais e permite tentar novamente',async t=>{
  setActivePinia(createPinia());const store=useUploadStore();await store.importarArquivo(arquivo())
  adapters(t,async()=>{throw new Error('offline')})
  assert.equal(await store.enviarDados(),null)
  assert.equal(store.totalRegistros,3)
  assert.equal(store.enviando,false)
  assert.match(store.erroEnvio,/conectar/)
  assert.equal(store.analiseSalva,null)
})
