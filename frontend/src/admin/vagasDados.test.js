import assert from 'node:assert/strict'
import test from 'node:test'
import { montarCaminhoVagas } from './vagasDados.js'

test('monta paginação sem filtros vazios', () => {
  assert.equal(
    montarCaminhoVagas({ status: '', fonte: '', busca: '   ' }, 2),
    '/vagas?page=2&size=20',
  )
})

test('codifica os filtros de moderação', () => {
  assert.equal(
    montarCaminhoVagas({ status: 'OCULTA', fonte: 'GUPY', busca: ' java júnior ' }, 0),
    '/vagas?page=0&size=20&status=OCULTA&fonte=GUPY&busca=java+j%C3%BAnior',
  )
})
