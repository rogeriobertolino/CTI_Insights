const CONNECTION_ERROR = 'Não foi possível conectar à API. Verifique se o backend está em execução e tente novamente.'
const ROUTING_ERROR = 'O endereço configurado para a API está respondendo como uma página, não como o serviço de análise. Verifique o endereço e a porta do backend.'

export async function requestApi(url, options) {
  let response
  try {
    response = await fetch(url, options)
  } catch {
    throw new Error(CONNECTION_ERROR)
  }

  const contentType = response.headers.get('content-type') || ''
  const isJson = /\bapplication\/(?:[\w.-]+\+)?json\b/i.test(contentType)
  if (!isJson) {
    if (response.status === 501) {
      throw new Error('O servidor configurado para a API não aceita o envio de arquivos (501). Verifique se o endereço e a porta apontam para o backend de análise.')
    }
    if ([500, 502, 503, 504].includes(response.status)) throw new Error(CONNECTION_ERROR)
    throw new Error(ROUTING_ERROR)
  }

  let body
  try {
    body = await response.json()
  } catch {
    throw new Error('A API devolveu uma resposta inválida. Tente novamente ou verifique o backend.')
  }
  if (!response.ok) {
    throw new Error(typeof body?.message === 'string' && body.message ? body.message : `Falha na API (${response.status}).`)
  }
  return body
}
