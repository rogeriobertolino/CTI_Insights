import assert from 'node:assert/strict'
import { mock, test } from 'node:test'
import { requestApi } from '../../src/services/requestApi.js'

function mockFetch(t, implementation) {
  const fetchMock = mock.method(globalThis, 'fetch', implementation)
  t.after(() => fetchMock.mock.restore())
  return fetchMock
}

function htmlResponse(status = 200) {
  return new Response('<!doctype html><html><body>Server response</body></html>', {
    status,
    headers: { 'content-type': 'text/html; charset=utf-8' },
  })
}

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  })
}

test('501 HTML explains that the API address or port is incorrect', async (t) => {
  mockFetch(t, async () => htmlResponse(501))

  await assert.rejects(requestApi('/api/upload', { method: 'POST' }), (error) => {
    assert.match(error.message, /501/)
    assert.match(error.message, /endere[çc]o|porta|servidor/i)
    assert.doesNotMatch(error.message, /<!doctype|<html>/i)
    return true
  })
})

test('200 HTML rejects instead of turning a frontend fallback page into empty data', async (t) => {
  mockFetch(t, async () => htmlResponse())

  await assert.rejects(requestApi('/api/data'), (error) => {
    assert.match(error.message, /JSON|API|HTML/i)
    assert.doesNotMatch(error.message, /<!doctype|<html>/i)
    return true
  })
})

test('invalid JSON rejects with an actionable response error', async (t) => {
  mockFetch(t, async () => new Response('{ invalid json', {
    headers: { 'content-type': 'application/json' },
  }))

  await assert.rejects(requestApi('/api/data'), (error) => {
    assert.match(error.message, /JSON|resposta/i)
    return true
  })
})

for (const status of [500, 502, 503, 504]) {
  test(`${status} HTML explains backend connectivity`, async (t) => {
    mockFetch(t, async () => htmlResponse(status))

    await assert.rejects(requestApi('/api/data'), (error) => {
      assert.match(error.message, /conex|conect|indispon|backend|servidor/i)
      assert.doesNotMatch(error.message, /<!doctype|<html>/i)
      return true
    })
  })
}

test('400 JSON preserves the API message', async (t) => {
  const message = 'O arquivo enviado não possui as colunas esperadas.'
  mockFetch(t, async () => jsonResponse({ message }, 400))

  await assert.rejects(requestApi('/api/upload', { method: 'POST' }), { message })
})

test('500 JSON without a message uses an error containing its status', async (t) => {
  mockFetch(t, async () => jsonResponse({ detail: 'Internal server error' }, 500))

  await assert.rejects(requestApi('/api/data'), (error) => {
    assert.match(error.message, /500/)
    return true
  })
})

test('JSON success returns the payload and forwards URL and options unchanged', async (t) => {
  const payload = { ok: true, items: [{ id: 1, name: 'Example' }] }
  const fetchMock = mockFetch(t, async () => jsonResponse(payload))
  const options = { method: 'GET', signal: new AbortController().signal }

  assert.deepEqual(await requestApi('/api/data?limit=10', options), payload)
  assert.equal(fetchMock.mock.callCount(), 1)
  assert.equal(fetchMock.mock.calls[0].arguments[0], '/api/data?limit=10')
  assert.equal(fetchMock.mock.calls[0].arguments[1], options)
})

test('FormData uploads retain their body without a manually assigned content type', async (t) => {
  const fetchMock = mockFetch(t, async () => jsonResponse({ ok: true }))
  const body = new FormData()
  body.append('file', new Blob(['column\nvalue'], { type: 'text/csv' }), 'data.csv')
  const options = { method: 'POST', body }

  assert.deepEqual(await requestApi('/api/upload', options), { ok: true })
  const [url, forwardedOptions] = fetchMock.mock.calls[0].arguments
  assert.equal(url, '/api/upload')
  assert.equal(forwardedOptions, options)
  assert.equal(forwardedOptions.body, body)
  assert.equal(forwardedOptions.headers, undefined)
})

test('fetch rejection produces an understandable connection diagnosis', async (t) => {
  mockFetch(t, async () => {
    throw new TypeError('Failed to fetch')
  })

  await assert.rejects(requestApi('/api/data'), (error) => {
    assert.match(error.message, /conex|conect|indispon|backend|servidor/i)
    return true
  })
})