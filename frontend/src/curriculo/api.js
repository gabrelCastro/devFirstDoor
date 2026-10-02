import { baixarArquivo, chamarApi } from '../conta/sessao.js'

const BASE = '/api/curriculo'

export const obterPerfil = () => chamarApi(`${BASE}/perfil`)
export const salvarPerfil = (perfil) => chamarApi(`${BASE}/perfil`, { method: 'PUT', corpo: perfil })
export const obterStatus = () => chamarApi(`${BASE}/status`)
export const listarVersoes = () => chamarApi(`${BASE}/versoes`)
export const obterVersao = (id) => chamarApi(`${BASE}/versoes/${id}`)
export const criarVersao = (descricaoVaga, candidaturaId) =>
  chamarApi(`${BASE}/versoes`, { method: 'POST', corpo: { descricaoVaga, candidaturaId: candidaturaId ?? null } })
export const salvarEscolhas = (id, escolhas) => chamarApi(`${BASE}/versoes/${id}/escolhas`, { method: 'PUT', corpo: escolhas })
export const excluirVersao = (id) => chamarApi(`${BASE}/versoes/${id}`, { method: 'DELETE' })

export const baixarPerfil = (formato) => baixarArquivo(`${BASE}/perfil/${formato}`, `curriculo.${formato}`)
export const baixarVersao = (id, formato) => baixarArquivo(`${BASE}/versoes/${id}/${formato}`, `curriculo.${formato}`)
