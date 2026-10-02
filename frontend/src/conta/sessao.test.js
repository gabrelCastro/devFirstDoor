import assert from 'node:assert/strict'
import test from 'node:test'
import { ErroNaoAutenticado, chamarApi, entrar, lerToken } from './sessao.js'

function localStorageFalso() {
  const dados = new Map()
  return {
    getItem: (chave) => (dados.has(chave) ? dados.get(chave) : null),
    setItem: (chave, valor) => dados.set(chave, String(valor)),
    removeItem: (chave) => dados.delete(chave),
  }
}

function prepararAmbiente(t, fetchFalso) {
  const fetchOriginal = globalThis.fetch
  const storageOriginal = Object.getOwnPropertyDescriptor(globalThis, 'localStorage')
  Object.defineProperty(globalThis, 'localStorage', { value: localStorageFalso(), configurable: true })
  globalThis.fetch = fetchFalso
  t.after(() => {
    globalThis.fetch = fetchOriginal
    if (storageOriginal) Object.defineProperty(globalThis, 'localStorage', storageOriginal)
    else delete globalThis.localStorage
  })
}

function json(corpo, status = 200) {
  return new Response(JSON.stringify(corpo), { status, headers: { 'Content-Type': 'application/json' } })
}

test('login guarda o token e as chamadas seguintes mandam Bearer', async (t) => {
  const chamadas = []
  prepararAmbiente(t, async (url, opcoes) => {
    chamadas.push({ url, opcoes })
    if (url === '/api/auth/login') return json({ token: 'abc', usuario: { usuario: 'joao' } })
    return json({ ok: true })
  })

  const usuario = await entrar('joao', 'senha-forte')
  assert.deepEqual(usuario, { usuario: 'joao' })
  assert.equal(lerToken(), 'abc')

  await chamarApi('/api/conta')
  assert.equal(chamadas[1].opcoes.headers.Authorization, 'Bearer abc')
})

test('401 com o token salvo apaga a sessão', async (t) => {
  prepararAmbiente(t, async (url) =>
    url === '/api/auth/login' ? json({ token: 'abc', usuario: {} }) : new Response(null, { status: 401 }),
  )
  await entrar('joao', 'senha-forte')

  await assert.rejects(chamarApi('/api/conta'), ErroNaoAutenticado)
  assert.equal(lerToken(), null)
})

test('login recusado repassa a mensagem do backend sem guardar token', async (t) => {
  prepararAmbiente(t, async () => json({ mensagem: 'Usuário ou senha incorretos' }, 401))

  await assert.rejects(entrar('joao', 'errada'), /Usuário ou senha incorretos/)
  assert.equal(lerToken(), null)
})
