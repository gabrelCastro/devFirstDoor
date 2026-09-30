/**
 * O backend manda LocalDateTime sem fuso (horário do servidor) e às vezes com
 * microssegundos; o Date do navegador só garante até milissegundos.
 */
export function paraData(iso) {
  if (!iso) return null
  const data = new Date(iso.replace(/(\.\d{3})\d+/, '$1'))
  return Number.isNaN(data.getTime()) ? null : data
}

export function formatarDataHora(iso) {
  const data = paraData(iso)
  if (!data) return '—'
  return data.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatarHora(iso) {
  const data = paraData(iso)
  if (!data) return '—'
  return data.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

export function formatarDuracao(inicioIso, fimIso) {
  const inicio = paraData(inicioIso)
  const fim = paraData(fimIso)
  if (!inicio || !fim) return '—'
  const segundos = Math.max(0, Math.round((fim - inicio) / 1000))
  if (segundos < 60) return `${segundos}s`
  const minutos = Math.floor(segundos / 60)
  if (minutos < 60) return `${minutos}min ${String(segundos % 60).padStart(2, '0')}s`
  return `${Math.floor(minutos / 60)}h ${String(minutos % 60).padStart(2, '0')}min`
}

/** "há 5 min" / "em 20 min", para o painel ler de relance. */
export function formatarRelativo(iso, agora = Date.now()) {
  const data = paraData(iso)
  if (!data) return ''
  const diferencaMin = Math.round((data.getTime() - agora) / 60000)
  const absoluto = Math.abs(diferencaMin)
  let texto
  if (absoluto < 1) return 'agora'
  if (absoluto < 60) texto = `${absoluto} min`
  else if (absoluto < 48 * 60) texto = `${Math.round(absoluto / 60)} h`
  else texto = `${Math.round(absoluto / (24 * 60))} dias`
  return diferencaMin > 0 ? `em ${texto}` : `há ${texto}`
}

export const SAUDE_LABEL = {
  OK: 'ok',
  ALERTA: 'alerta',
  FALHA: 'falha',
  SEM_DADOS: 'sem dados',
  DESLIGADA: 'desligada',
}

export const STATUS_EXECUCAO_LABEL = {
  EM_ANDAMENTO: 'em andamento',
  SUCESSO: 'sucesso',
  PARCIAL: 'parcial',
  ERRO: 'erro',
}

export const ORIGEM_LABEL = {
  INICIAL: 'inicial',
  AGENDADA: 'agendada',
  MANUAL: 'manual',
}
