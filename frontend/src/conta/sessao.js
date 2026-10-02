// Sessão do site inteiro (página pública, candidaturas e admin). Guarda só o token de sessão:
// a senha nunca fica no navegador. O token vale 30 dias e o logout o revoga no servidor.
const CHAVE_TOKEN = 'sessao'

/** Evento disparado na janela quando o servidor recusa o token salvo (expirou ou foi revogado). */
export const EVENTO_SESSAO_EXPIRADA = 'sessao-expirada'

/** Credencial ausente, expirada ou revogada (401). */
export class ErroNaoAutenticado extends Error {
  constructor() {
    super('sessão expirada ou credencial inválida')
    this.name = 'ErroNaoAutenticado'
  }
}

/** Sessão válida, mas sem o papel exigido pela rota (403). */
export class ErroSemPermissao extends Error {
  constructor() {
    super('sua conta não tem permissão para esta ação')
    this.name = 'ErroSemPermissao'
  }
}

export function lerToken() {
  try {
    return localStorage.getItem(CHAVE_TOKEN)
  } catch {
    return null
  }
}

function salvarToken(token) {
  try {
    localStorage.setItem(CHAVE_TOKEN, token)
  } catch {
    // sem localStorage o login vale só enquanto a página estiver aberta
  }
}

export function apagarToken() {
  try {
    localStorage.removeItem(CHAVE_TOKEN)
  } catch {
    // nada a apagar
  }
}

function avisarExpiracao() {
  if (typeof window !== 'undefined') window.dispatchEvent(new Event(EVENTO_SESSAO_EXPIRADA))
}

/**
 * Chama a API com o token da sessão. Devolve o JSON da resposta (ou null quando não há corpo).
 * 401 vira {@link ErroNaoAutenticado} e apaga o token salvo; 403 vira {@link ErroSemPermissao};
 * os demais erros trazem a `mensagem` do backend, quando houver.
 */
export async function chamarApi(caminho, { method = 'GET', corpo, token = lerToken() } = {}) {
  const headers = token ? { Authorization: `Bearer ${token}` } : {}
  if (corpo !== undefined) headers['Content-Type'] = 'application/json'
  const resposta = await fetch(caminho, {
    method,
    headers,
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  })
  if (resposta.status === 401) {
    if (token && token === lerToken()) {
      apagarToken()
      avisarExpiracao()
    }
    throw new ErroNaoAutenticado()
  }
  if (resposta.status === 403) {
    throw new ErroSemPermissao()
  }
  const respostaCorpo = await resposta.json().catch(() => null)
  if (!resposta.ok) {
    const erro = new Error(
      respostaCorpo?.mensagem ??
        respostaCorpo?.detail ??
        `API respondeu com status ${resposta.status}`,
    )
    // Quem chama às vezes precisa do resto do corpo (ex.: o id no 409 de candidatura repetida).
    erro.status = resposta.status
    erro.corpo = respostaCorpo
    throw erro
  }
  return respostaCorpo
}

/** Troca usuário e senha por um token e o guarda. Devolve a conta. */
export async function entrar(usuario, senha) {
  const resposta = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ usuario, senha }),
  })
  const corpo = await resposta.json().catch(() => null)
  if (!resposta.ok) {
    throw new Error(corpo?.mensagem ?? `API respondeu com status ${resposta.status}`)
  }
  salvarToken(corpo.token)
  return corpo.usuario
}

export async function cadastrar(usuario, senha) {
  await chamarApi('/api/conta', { method: 'POST', corpo: { usuario, senha }, token: null })
  return entrar(usuario, senha)
}

/** Revoga o token no servidor e o apaga daqui, mesmo se o servidor estiver fora do ar. */
export async function sair() {
  const token = lerToken()
  apagarToken()
  if (!token) return
  await chamarApi('/api/auth/logout', { method: 'POST', token }).catch(() => {})
}

/**
 * Baixa um arquivo de uma rota autenticada (o token não vai num link comum) e entrega ao navegador
 * com o nome que o servidor mandou no Content-Disposition.
 */
export async function baixarArquivo(caminho, nomePadrao) {
  const token = lerToken()
  const resposta = await fetch(caminho, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
  if (resposta.status === 401) {
    if (token) {
      apagarToken()
      avisarExpiracao()
    }
    throw new ErroNaoAutenticado()
  }
  if (!resposta.ok) {
    const corpo = await resposta.json().catch(() => null)
    throw new Error(corpo?.mensagem ?? `API respondeu com status ${resposta.status}`)
  }
  const nome = /filename="([^"]+)"/.exec(resposta.headers.get('Content-Disposition') ?? '')?.[1] ?? nomePadrao
  const url = URL.createObjectURL(await resposta.blob())
  const link = document.createElement('a')
  link.href = url
  link.download = nome
  document.body.append(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
