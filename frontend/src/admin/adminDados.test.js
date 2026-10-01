import assert from 'node:assert/strict'
import test from 'node:test'
import { montarCaminhoDescartes, prepararSerieNovas, somarValores } from './adminDados.js'

test('monta paginação de descartes sem filtros vazios', () => {
  assert.equal(
    montarCaminhoDescartes({ fonte: '', motivo: '', busca: '   ' }, 2),
    '/descartes?page=2&size=20',
  )
})

test('codifica os filtros de descartes', () => {
  assert.equal(
    montarCaminhoDescartes({ fonte: 'GUPY', motivo: 'NAO_JAVA', busca: ' java júnior ' }, 0),
    '/descartes?page=0&size=20&fonte=GUPY&motivo=NAO_JAVA&busca=java+j%C3%BAnior',
  )
})

test('prepara trinta dias e soma as fontes na série de vagas novas', () => {
  const serie = prepararSerieNovas(
    [
      { data: '2026-09-30', fonte: 'GUPY', total: 2 },
      { data: '2026-09-30', fonte: 'LINKEDIN', total: 3 },
      { data: '2026-10-01', fonte: 'GUPY', total: 1 },
    ],
    new Date('2026-10-01T12:00:00Z'),
  )

  assert.equal(serie.length, 30)
  assert.deepEqual(serie.at(-2), { data: '2026-09-30', total: 5 })
  assert.deepEqual(serie.at(-1), { data: '2026-10-01', total: 1 })
  assert.equal(somarValores({ GUPY: 2, LINKEDIN: 3 }), 5)
})
