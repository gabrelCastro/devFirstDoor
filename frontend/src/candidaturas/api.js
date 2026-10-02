import { baixarArquivo, chamarApi } from '../conta/sessao.js'

const BASE = '/api/candidaturas'

export const listarCandidaturas = () => chamarApi(BASE)
export const buscarResumo = () => chamarApi(`${BASE}/resumo`)
export const detalharCandidatura = (id) => chamarApi(`${BASE}/${id}`)
export const criarCandidatura = (corpo) => chamarApi(BASE, { method: 'POST', corpo })
export const atualizarCandidatura = (id, corpo) => chamarApi(`${BASE}/${id}`, { method: 'PATCH', corpo })
export const definirProximoPasso = (id, texto, data) =>
  chamarApi(`${BASE}/${id}/proximo-passo`, { method: 'PUT', corpo: { texto, data } })
export const excluirCandidatura = (id) => chamarApi(`${BASE}/${id}`, { method: 'DELETE' })
export const adicionarNota = (id, texto) => chamarApi(`${BASE}/${id}/notas`, { method: 'POST', corpo: { texto } })
export const apagarNota = (id, notaId) => chamarApi(`${BASE}/${id}/notas/${notaId}`, { method: 'DELETE' })

/** O CSV exige o token, então não dá para ser um link comum. */
export const baixarCsv = () => baixarArquivo(`${BASE}/exportar`, 'candidaturas.csv')
