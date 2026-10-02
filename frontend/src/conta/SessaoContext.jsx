import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  EVENTO_SESSAO_EXPIRADA,
  ErroNaoAutenticado,
  cadastrar as cadastrarNaApi,
  chamarApi,
  entrar as entrarNaApi,
  lerToken,
  sair as sairNaApi,
} from './sessao'
import ModalEntrar from './ModalEntrar'
import { SessaoContexto } from './useSessao'

/**
 * Conta logada no site. Valida o token salvo ao abrir a página e oferece {@code pedirLogin}, que
 * abre o modal de entrar e, depois do login, executa a ação que o usuário tinha tentado fazer.
 */
export function SessaoProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  const [verificando, setVerificando] = useState(() => lerToken() != null)
  const [pedido, setPedido] = useState(null)

  useEffect(() => {
    if (!verificando) return
    let cancelado = false
    chamarApi('/api/conta')
      .then((conta) => {
        if (!cancelado) setUsuario(conta)
      })
      .catch((e) => {
        // Fora do ar não desloga: o token continua salvo para a próxima tentativa.
        if (!cancelado && !(e instanceof ErroNaoAutenticado)) console.warn('Não foi possível validar a sessão', e)
      })
      .finally(() => {
        if (!cancelado) setVerificando(false)
      })
    return () => {
      cancelado = true
    }
  }, [verificando])

  useEffect(() => {
    const aoExpirar = () => setUsuario(null)
    window.addEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar)
    return () => window.removeEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar)
  }, [])

  const entrar = useCallback(async (login, senha) => {
    const conta = await entrarNaApi(login, senha)
    setUsuario(conta)
    return conta
  }, [])

  const cadastrar = useCallback(async (login, senha) => {
    const conta = await cadastrarNaApi(login, senha)
    setUsuario(conta)
    return conta
  }, [])

  const sair = useCallback(async () => {
    setUsuario(null)
    await sairNaApi()
  }, [])

  /** Abre o modal de entrar; {@code aoEntrar} roda depois de um login ou cadastro bem-sucedido. */
  const pedirLogin = useCallback((opcoes = {}) => setPedido(opcoes), [])

  const valor = useMemo(
    () => ({ usuario, verificando, entrar, cadastrar, sair, pedirLogin }),
    [usuario, verificando, entrar, cadastrar, sair, pedirLogin],
  )

  function concluirLogin(conta) {
    const acao = pedido?.aoEntrar
    setPedido(null)
    acao?.(conta)
  }

  return (
    <SessaoContexto.Provider value={valor}>
      {children}
      {pedido && (
        <ModalEntrar
          motivo={pedido.motivo}
          abaInicial={pedido.aba}
          entrar={entrar}
          cadastrar={cadastrar}
          aoConcluir={concluirLogin}
          aoFechar={() => setPedido(null)}
        />
      )}
    </SessaoContexto.Provider>
  )
}
