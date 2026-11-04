import {test} from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import {createPinia,setActivePinia} from 'pinia'
import * as XLSX from 'xlsx'
import {useUploadStore} from '../../src/stores/uploadStore.js'

test('amostra anual: mesmas linhas, média, serviços e contagens da referência',async()=>{
  setActivePinia(createPinia())
  const store=useUploadStore(), bytes=fs.readFileSync(new URL('../../sample-data/dados.xlsx',import.meta.url))
  assert.equal(await store.importarArquivo({name:'dados.xlsx',size:bytes.length,arrayBuffer:async()=>bytes.buffer.slice(bytes.byteOffset,bytes.byteOffset+bytes.byteLength)}),true)
  assert.equal(store.nomeAba,'upload_clientes'); assert.equal(store.totalRegistros,38); assert.equal(store.totalValidos,32)
  assert.deepEqual([...new Set(store.erros.map(e=>e.linha))],[32,33,34,37,38,39])
  assert.equal(store.faturamentoMedio,2261250)
  assert.equal(store.clientesValidos.reduce((n,r)=>n+r.servicos_contratados.split(';').length,0),61)
  assert.equal(new Set(store.clientesValidos.map(r=>r.consultor)).size,4)
  assert.equal(new Set(store.clientesValidos.map(r=>r.segmento)).size,6)
  assert.ok(store.avisos.length)
  const output=store.prepararArquivoEnvio(), wb=XLSX.read(await output.arrayBuffer())
  const rows=XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]])
  assert.equal(rows.length,32); assert.ok(rows.every(r=>/^\d{4}-\d{2}-\d{2}$/.test(r.data_contratacao)))
  assert.equal(rows.find(r=>r.linha_origem===35).nivel,'B')
})

test('aba nomeada tem prioridade e campos anuais ausentes são recusados',async()=>{
  setActivePinia(createPinia());const store=useUploadStore()
  const wb=XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb,XLSX.utils.aoa_to_sheet([['Instruções']]),'LEIA-ME')
  XLSX.utils.book_append_sheet(wb,XLSX.utils.aoa_to_sheet([['codigo_cliente','nome_cliente','segmento','servicos_contratados'],['1','Empresa','Indústria','Internet']]),'upload_clientes')
  const bytes=XLSX.write(wb,{type:'array',bookType:'xlsx'})
  assert.equal(await store.importarArquivo({name:'base.xlsx',size:bytes.byteLength,arrayBuffer:async()=>bytes}),false)
  assert.match(store.erroArquivo,/consultor/)
})
