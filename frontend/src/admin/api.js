const CHAVE_CREDENCIAL = 'admin-credencial'

/** Erro de credencial ausente ou inválida: a tela volta para o login. */
export class ErroNaoAutenticado extends Error {
  constructor() {
    super('sessão expirada ou credencial inválida')
    this.name = 'ErroNaoAutenticado'
  }
}

/** Codifica em base64 passando por UTF-8, porque o btoa sozinho quebra com acentos. */
function base64(texto) {
  const bytes = new TextEncoder().encode(texto)
  let binario = ''
  for (const byte of bytes) binario += String.fromCharCode(byte)
  return btoa(binario)
}

export function montarCredencial(usuario, senha) {
  return `Basic ${base64(`${usuario}:${senha}`)}`
}

// Só sessionStorage: a credencial some ao fechar a aba.
export function lerCredencial() {
  try {
    return sessionStorage.getItem(CHAVE_CREDENCIAL)
  } catch {
    return null
  }
}

export function salvarCredencial(credencial) {
  try {
    sessionStorage.setItem(CHAVE_CREDENCIAL, credencial)
  } catch {
    // sem sessionStorage o login vale só enquanto a página estiver aberta
  }
}

export function apagarCredencial() {
  try {
    sessionStorage.removeItem(CHAVE_CREDENCIAL)
  } catch {
    // nada a apagar
  }
}

/**
 * Chama um endpoint de /api/admin. Devolve o JSON da resposta; 401 vira
 * {@link ErroNaoAutenticado} e os demais erros trazem a `mensagem` do backend, quando houver.
 */
export async function chamarAdmin(
  caminho,
  { method = 'GET', credencial = lerCredencial(), corpo } = {},
) {
  const headers = credencial ? { Authorization: credencial } : {}
  if (corpo !== undefined) headers['Content-Type'] = 'application/json'
  const resposta = await fetch(`/api/admin${caminho}`, {
    method,
    headers,
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  })
  if (resposta.status === 401) {
    throw new ErroNaoAutenticado()
  }
  const respostaCorpo = await resposta.json().catch(() => null)
  if (!resposta.ok) {
    throw new Error(
      respostaCorpo?.mensagem ??
        respostaCorpo?.detail ??
        `API respondeu com status ${resposta.status}`,
    )
  }
  return respostaCorpo
}
