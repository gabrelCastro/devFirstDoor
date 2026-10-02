import assert from 'node:assert/strict'
import test from 'node:test'
import { ErroNaoAutenticado, ErroSemPermissao, chamarAdmin } from './api.js'

test('envia corpo JSON com a credencial administrativa', async (t) => {
  const fetchOriginal = globalThis.fetch
  t.after(() => {
    globalThis.fetch = fetchOriginal
  })
  globalThis.fetch = async (url, opcoes) => {
    assert.equal(url, '/api/admin/configuracao')
    assert.deepEqual(opcoes, {
      method: 'PUT',
      headers: {
        Authorization: 'Bearer teste',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ intervaloColetaMinutos: 30 }),
    })
    return new Response(JSON.stringify({ salvo: true }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  }

  const resposta = await chamarAdmin('/configuracao', {
    method: 'PUT',
    token: 'teste',
    corpo: { intervaloColetaMinutos: 30 },
  })

  assert.deepEqual(resposta, { salvo: true })
})

test('repassa a mensagem de validação devolvida pelo backend', async (t) => {
  const fetchOriginal = globalThis.fetch
  t.after(() => {
    globalThis.fetch = fetchOriginal
  })
  globalThis.fetch = async () =>
    new Response(JSON.stringify({ mensagem: 'O intervalo deve ser de pelo menos 30 minutos' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json' },
    })

  await assert.rejects(
    chamarAdmin('/configuracao', { method: 'PUT', token: 'teste', corpo: {} }),
    /O intervalo deve ser de pelo menos 30 minutos/,
  )
})

test('transforma resposta 401 em erro de autenticação', async (t) => {
  const fetchOriginal = globalThis.fetch
  t.after(() => {
    globalThis.fetch = fetchOriginal
  })
  globalThis.fetch = async () => new Response(null, { status: 401 })

  await assert.rejects(
    chamarAdmin('/vagas', { credencial: 'Basic expirada' }),
    ErroNaoAutenticado,
  )
})

test('conta sem papel ADMIN vira ErroSemPermissao', async (t) => {
  const fetchOriginal = globalThis.fetch
  t.after(() => {
    globalThis.fetch = fetchOriginal
  })
  globalThis.fetch = async () => new Response(null, { status: 403 })

  await assert.rejects(chamarAdmin('/me', { token: 'teste' }), ErroSemPermissao)
})
