import { chamarApi } from '../conta/sessao.js'

export { ErroNaoAutenticado, ErroSemPermissao } from '../conta/sessao.js'

/** Chama um endpoint de /api/admin com o token da sessão (ver {@link chamarApi}). */
export function chamarAdmin(caminho, opcoes) {
  return chamarApi(`/api/admin${caminho}`, opcoes)
}
