export const NIVEL_LABEL = {
  ESTAGIO: 'Estágio',
  JUNIOR: 'Júnior',
}

const JANELA_NOVO_MS = 48 * 60 * 60 * 1000

export function formatarData(isoDate) {
  if (!isoDate) return '—'
  const [ano, mes, dia] = isoDate.split('-')
  return `${dia}/${mes}/${ano}`
}

export function formatarIndice(indice) {
  return String(indice).padStart(3, '0')
}

export function ehRecente(dataColetaIso) {
  if (!dataColetaIso) return false
  const coletaEm = new Date(dataColetaIso).getTime()
  if (Number.isNaN(coletaEm)) return false
  return Date.now() - coletaEm < JANELA_NOVO_MS
}
