export const FONTES_VAGAS = ['GUPY', 'PROGRAMATHOR', 'REMOTEOK', 'LINKEDIN', 'GREENHOUSE', 'LEVER']

export const STATUS_VAGA_LABEL = {
  ATIVA: 'ativa',
  EXPIRADA: 'expirada',
  OCULTA: 'oculta',
}

export const NIVEL_VAGA_LABEL = {
  ESTAGIO: 'estágio',
  JUNIOR: 'júnior',
}

export function montarCaminhoVagas(filtros, pagina, tamanho = 20) {
  const parametros = new URLSearchParams({ page: String(pagina), size: String(tamanho) })
  if (filtros.status) parametros.set('status', filtros.status)
  if (filtros.fonte) parametros.set('fonte', filtros.fonte)
  if (filtros.busca.trim()) parametros.set('busca', filtros.busca.trim())
  return `/vagas?${parametros}`
}
