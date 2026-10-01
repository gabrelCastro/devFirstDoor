export const FONTES_ADMIN = ['GUPY', 'PROGRAMATHOR', 'REMOTEOK', 'LINKEDIN', 'GREENHOUSE', 'LEVER']

export const MOTIVO_DESCARTE_LABEL = {
  NAO_JAVA: 'não menciona Java',
  NAO_REMOTA: 'não remota',
  NIVEL: 'nível incompatível',
  FORA_DE_TECNOLOGIA: 'fora de tecnologia',
  ERRO_LEITURA: 'erro de leitura',
}

export function montarCaminhoDescartes(filtros, pagina, tamanho = 20) {
  const parametros = new URLSearchParams({ page: String(pagina), size: String(tamanho) })
  if (filtros.fonte) parametros.set('fonte', filtros.fonte)
  if (filtros.motivo) parametros.set('motivo', filtros.motivo)
  if (filtros.busca.trim()) parametros.set('busca', filtros.busca.trim())
  return `/descartes?${parametros}`
}

export function somarValores(valores = {}) {
  return Object.values(valores).reduce((total, valor) => total + valor, 0)
}

/** Preenche os dias sem vagas para o gráfico não ligar pontos através de lacunas. */
export function prepararSerieNovas(registros, hoje = new Date()) {
  const totais = new Map()
  for (const registro of registros ?? []) {
    totais.set(registro.data, (totais.get(registro.data) ?? 0) + registro.total)
  }

  const fim = new Date(Date.UTC(hoje.getFullYear(), hoje.getMonth(), hoje.getDate()))
  return Array.from({ length: 30 }, (_, indice) => {
    const data = new Date(fim)
    data.setUTCDate(fim.getUTCDate() - (29 - indice))
    const chave = data.toISOString().slice(0, 10)
    return { data: chave, total: totais.get(chave) ?? 0 }
  })
}
