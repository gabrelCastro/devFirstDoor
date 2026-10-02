// Regras de exibição das candidaturas, sem React: compartilhadas pela página pública e pelo
// quadro, e testadas com node --test.

export const ETAPAS = [
  { id: 'INTERESSE', rotulo: 'interesse', descricao: 'Quero me candidatar', encerrada: false },
  { id: 'CANDIDATADO', rotulo: 'candidatei', descricao: 'Candidatura enviada', encerrada: false },
  { id: 'TESTE_TECNICO', rotulo: 'teste técnico', descricao: 'Desafio ou prova', encerrada: false },
  { id: 'ENTREVISTA', rotulo: 'entrevista', descricao: 'Conversas com a empresa', encerrada: false },
  { id: 'OFERTA', rotulo: 'oferta', descricao: 'Proposta recebida', encerrada: false },
  { id: 'CONTRATADO', rotulo: 'contratado', descricao: 'Deu certo!', encerrada: true },
  { id: 'REPROVADO', rotulo: 'reprovado', descricao: 'A empresa encerrou', encerrada: true },
  { id: 'DESISTENCIA', rotulo: 'desisti', descricao: 'Eu encerrei', encerrada: true },
  { id: 'SEM_RETORNO', rotulo: 'sem retorno', descricao: 'A empresa sumiu', encerrada: true },
]

export const ETAPAS_ATIVAS = ETAPAS.filter((etapa) => !etapa.encerrada)
export const ETAPAS_ENCERRADAS = ETAPAS.filter((etapa) => etapa.encerrada)
export const ROTULO_ETAPA = Object.fromEntries(ETAPAS.map((etapa) => [etapa.id, etapa.rotulo]))

/** Igual ao backend (CandidaturaService.DIAS_PARA_FOLLOW_UP). */
export const DIAS_PARA_FOLLOW_UP = 14

const DIA_MS = 24 * 60 * 60 * 1000

export function ehEncerrada(etapa) {
  return ETAPAS_ENCERRADAS.some((item) => item.id === etapa)
}

/** LocalDateTime do backend (sem fuso, às vezes com microssegundos) para Date. */
export function paraData(iso) {
  if (!iso) return null
  const data = new Date(iso.replace(/(\.\d{3})\d+/, '$1'))
  return Number.isNaN(data.getTime()) ? null : data
}

/** LocalDate ("2026-10-05") para meia-noite local. */
function paraDia(isoData) {
  if (!isoData) return null
  const [ano, mes, dia] = isoData.split('-').map(Number)
  return new Date(ano, mes - 1, dia)
}

function inicioDoDia(data) {
  return new Date(data.getFullYear(), data.getMonth(), data.getDate())
}

/** Dias corridos entre a data e hoje, contando por dia de calendário. */
export function diasDesde(iso, agora = new Date()) {
  const data = paraData(iso)
  if (!data) return null
  return Math.max(0, Math.round((inicioDoDia(agora) - inicioDoDia(data)) / DIA_MS))
}

export function formatarDias(dias) {
  if (dias == null) return '—'
  if (dias === 0) return 'hoje'
  if (dias === 1) return 'há 1 dia'
  return `há ${dias} dias`
}

/** "05/10" ou "05/10/2025" quando não é do ano corrente. */
export function formatarDiaCurto(isoData, agora = new Date()) {
  const dia = paraDia(isoData)
  if (!dia) return '—'
  const base = `${String(dia.getDate()).padStart(2, '0')}/${String(dia.getMonth() + 1).padStart(2, '0')}`
  return dia.getFullYear() === agora.getFullYear() ? base : `${base}/${dia.getFullYear()}`
}

/** 'vencido' | 'hoje' | 'futuro' | null, para colorir o próximo passo. */
export function situacaoProximoPasso(candidatura, agora = new Date()) {
  const dia = paraDia(candidatura.dataProximoPasso)
  if (!dia) return null
  const hoje = inicioDoDia(agora)
  if (dia < hoje) return 'vencido'
  if (dia.getTime() === hoje.getTime()) return 'hoje'
  return 'futuro'
}

/** Espelha CandidaturaService.precisaFollowUp: parada em "candidatei" ou com passo vencido. */
export function precisaFollowUp(candidatura, agora = new Date()) {
  if (ehEncerrada(candidatura.etapa)) return false
  const parada =
    candidatura.etapa === 'CANDIDATADO' && diasDesde(candidatura.etapaDesde, agora) >= DIAS_PARA_FOLLOW_UP
  return parada || situacaoProximoPasso(candidatura, agora) === 'vencido'
}

/** A vaga coletada saiu do ar na fonte (expirou ou foi ocultada). */
export function vagaSaiuDoAr(candidatura) {
  return candidatura.vagaStatus != null && candidatura.vagaStatus !== 'ATIVA'
}

function normalizar(texto) {
  return (texto ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
}

export function filtrarCandidaturas(candidaturas, termo) {
  const busca = normalizar(termo).trim()
  if (!busca) return candidaturas
  return candidaturas.filter((c) =>
    [c.titulo, c.empresa, c.local, c.proximoPasso].some((campo) => normalizar(campo).includes(busca)),
  )
}

/** { ETAPA: [candidaturas] } com todas as etapas presentes; mais recentes primeiro. */
export function agruparPorEtapa(candidaturas) {
  const grupos = Object.fromEntries(ETAPAS.map((etapa) => [etapa.id, []]))
  const ordenadas = [...candidaturas].sort(
    (a, b) => (paraData(b.atualizadaEm)?.getTime() ?? 0) - (paraData(a.atualizadaEm)?.getTime() ?? 0),
  )
  for (const candidatura of ordenadas) {
    grupos[candidatura.etapa]?.push(candidatura)
  }
  return grupos
}

/** vagaId → { id, etapa }, para a página pública marcar as vagas já acompanhadas. */
export function mapaPorVaga(candidaturas) {
  const mapa = new Map()
  for (const c of candidaturas) {
    if (c.vagaId != null) mapa.set(c.vagaId, { id: c.id, etapa: c.etapa })
  }
  return mapa
}

/** LocalDateTime "agora" no formato do backend, para atualizações otimistas. */
export function agoraIso(agora = new Date()) {
  const deslocamento = agora.getTimezoneOffset() * 60000
  return new Date(agora.getTime() - deslocamento).toISOString().slice(0, 19)
}

/** Aplica a mudança de etapa localmente, antes da resposta do servidor. */
export function moverLocalmente(candidaturas, id, etapa, agora = new Date()) {
  const quando = agoraIso(agora)
  return candidaturas.map((c) =>
    c.id === id ? { ...c, etapa, etapaDesde: quando, atualizadaEm: quando } : c,
  )
}

export function substituir(candidaturas, atualizada) {
  return candidaturas.map((c) => (c.id === atualizada.id ? atualizada : c))
}

export function formatarPercentual(taxa) {
  if (taxa == null) return '—'
  return `${Math.round(taxa * 100)}%`
}

export function formatarMediaDias(dias) {
  if (dias == null) return '—'
  const arredondado = Math.round(dias * 10) / 10
  return `${String(arredondado).replace('.', ',')} ${arredondado === 1 ? 'dia' : 'dias'}`
}
