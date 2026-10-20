import assert from 'node:assert/strict'
import { test } from 'node:test'
import { parseFaturamento } from '../src/utils/faturamento.js'
test('valores monetários aceitam números do Excel e formatos BR/US sem trocar a escala', () => {
  for (const [input, output] of [[707553.52,707553.52], ['707,553.52',707553.52], ['R$ 1.234,56',1234.56], ['1234.56',1234.56], ['1234,56',1234.56]]) assert.equal(parseFaturamento(input),output)
  for(const input of ['', 'abc', '1.2.3', '12,34,56', Infinity, 'NaN']) assert.equal(parseFaturamento(input),null)
})
