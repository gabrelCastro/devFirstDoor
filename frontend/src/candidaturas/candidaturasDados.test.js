import assert from 'node:assert/strict'
import test from 'node:test'
import {
  agruparPorEtapa,
  diasDesde,
  filtrarCandidaturas,
  formatarDiaCurto,
  formatarDias,
  formatarMediaDias,
  formatarPercentual,
  mapaPorVaga,
  moverLocalmente,
  precisaFollowUp,
  situacaoProximoPasso,
  vagaSaiuDoAr,
} from './candidaturasDados.js'

const AGORA = new Date(2026, 9, 7, 10, 0) // 07/10/2026 10:00 local

function candidatura(extra) {
  return {
    id: 1,
    vagaId: null,
    vagaStatus: null,
    titulo: 'Estágio Java',
    empresa: 'Açaí Tech',
    local: 'Remoto',
    etapa: 'INTERESSE',
    etapaDesde: '2026-10-07T09:00:00',
    atualizadaEm: '2026-10-07T09:00:00',
    dataProximoPasso: null,
    proximoPasso: null,
    ...extra,
  }
}

test('conta dias por dia de calendário, inclusive com microssegundos', () => {
  assert.equal(diasDesde('2026-10-07T00:01:00.123456', AGORA), 0)
  assert.equal(diasDesde('2026-10-06T23:59:00', AGORA), 1)
  assert.equal(diasDesde('2026-09-23T10:00:00', AGORA), 14)
  assert.equal(diasDesde(null, AGORA), null)
  assert.equal(formatarDias(0), 'hoje')
  assert.equal(formatarDias(1), 'há 1 dia')
  assert.equal(formatarDias(15), 'há 15 dias')
})

test('follow-up: candidatura parada há 14 dias ou próximo passo vencido', () => {
  assert.equal(precisaFollowUp(candidatura({ etapa: 'CANDIDATADO', etapaDesde: '2026-09-23T10:00:00' }), AGORA), true)
  assert.equal(precisaFollowUp(candidatura({ etapa: 'CANDIDATADO', etapaDesde: '2026-09-24T10:00:00' }), AGORA), false)
  assert.equal(precisaFollowUp(candidatura({ dataProximoPasso: '2026-10-06' }), AGORA), true)
  assert.equal(precisaFollowUp(candidatura({ dataProximoPasso: '2026-10-07' }), AGORA), false)
  assert.equal(
    precisaFollowUp(candidatura({ etapa: 'REPROVADO', dataProximoPasso: '2026-10-01' }), AGORA),
    false,
  )
})

test('situação do próximo passo', () => {
  assert.equal(situacaoProximoPasso(candidatura({ dataProximoPasso: '2026-10-06' }), AGORA), 'vencido')
  assert.equal(situacaoProximoPasso(candidatura({ dataProximoPasso: '2026-10-07' }), AGORA), 'hoje')
  assert.equal(situacaoProximoPasso(candidatura({ dataProximoPasso: '2026-10-08' }), AGORA), 'futuro')
  assert.equal(situacaoProximoPasso(candidatura({}), AGORA), null)
})

test('agrupa por etapa com todas as colunas e mais recentes primeiro', () => {
  const grupos = agruparPorEtapa([
    candidatura({ id: 1, atualizadaEm: '2026-10-01T10:00:00' }),
    candidatura({ id: 2, atualizadaEm: '2026-10-05T10:00:00' }),
    candidatura({ id: 3, etapa: 'OFERTA' }),
  ])
  assert.deepEqual(grupos.INTERESSE.map((c) => c.id), [2, 1])
  assert.deepEqual(grupos.OFERTA.map((c) => c.id), [3])
  assert.deepEqual(grupos.SEM_RETORNO, [])
})

test('busca ignora acentos e maiúsculas', () => {
  const lista = [candidatura({ id: 1 }), candidatura({ id: 2, empresa: 'Banco X', titulo: 'Dev Jr' })]
  assert.deepEqual(filtrarCandidaturas(lista, 'acai').map((c) => c.id), [1])
  assert.deepEqual(filtrarCandidaturas(lista, 'BANCO').map((c) => c.id), [2])
  assert.equal(filtrarCandidaturas(lista, '  ').length, 2)
})

test('mover localmente atualiza etapa e datas só da candidatura alvo', () => {
  const lista = [candidatura({ id: 1 }), candidatura({ id: 2 })]
  const movida = moverLocalmente(lista, 2, 'ENTREVISTA', AGORA)
  assert.equal(movida[0], lista[0])
  assert.equal(movida[1].etapa, 'ENTREVISTA')
  assert.equal(movida[1].etapaDesde, '2026-10-07T10:00:00')
})

test('mapa por vaga ignora as externas', () => {
  const mapa = mapaPorVaga([candidatura({ id: 1, vagaId: 10, etapa: 'OFERTA' }), candidatura({ id: 2 })])
  assert.deepEqual([...mapa.entries()], [[10, { id: 1, etapa: 'OFERTA' }]])
})

test('formatos curtos', () => {
  assert.equal(formatarDiaCurto('2026-10-05', AGORA), '05/10')
  assert.equal(formatarDiaCurto('2025-12-31', AGORA), '31/12/2025')
  assert.equal(formatarPercentual(0.456), '46%')
  assert.equal(formatarPercentual(null), '—')
  assert.equal(formatarMediaDias(4.25), '4,3 dias')
  assert.equal(formatarMediaDias(1), '1 dia')
  assert.equal(vagaSaiuDoAr(candidatura({ vagaStatus: 'EXPIRADA' })), true)
  assert.equal(vagaSaiuDoAr(candidatura({ vagaStatus: 'ATIVA' })), false)
})
