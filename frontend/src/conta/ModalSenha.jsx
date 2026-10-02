import { useRef, useState } from 'react'
import Dialogo from './Dialogo'
import { CampoSenha } from './Campos'
import { chamarApi } from './sessao'
import { SENHA_MIN, validarTrocaSenha } from './validacao'
import './conta.css'

const ORDEM_CAMPOS = ['atual', 'nova', 'confirmacao']

export default function ModalSenha({ aoFechar }) {
  const [valores, setValores] = useState({ atual: '', nova: '', confirmacao: '' })
  const [erros, setErros] = useState({})
  const [tentou, setTentou] = useState(false)
  const [erroGeral, setErroGeral] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [concluido, setConcluido] = useState(false)
  const refAtual = useRef(null)
  const refNova = useRef(null)
  const refConfirmacao = useRef(null)

  function alterar(campo, valor) {
    const novos = { ...valores, [campo]: valor }
    setValores(novos)
    setErroGeral(null)
    if (tentou) setErros(validarTrocaSenha(novos))
  }

  async function enviar(evento) {
    evento.preventDefault()
    const encontrados = validarTrocaSenha(valores)
    setTentou(true)
    setErros(encontrados)
    const primeiro = ORDEM_CAMPOS.find((campo) => encontrados[campo])
    if (primeiro) {
      const refs = { atual: refAtual, nova: refNova, confirmacao: refConfirmacao }
      refs[primeiro].current?.focus()
      return
    }
    setEnviando(true)
    setErroGeral(null)
    try {
      await chamarApi('/api/conta/senha', { method: 'PUT', corpo: { senhaAtual: valores.atual, novaSenha: valores.nova } })
      setConcluido(true)
    } catch (e) {
      setErroGeral(e.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Dialogo
      titulo={concluido ? 'Senha trocada' : 'Trocar senha'}
      subtitulo={concluido ? null : 'Os outros aparelhos em que você está logado serão desconectados.'}
      aoFechar={aoFechar}
    >
      {concluido ? (
        <div className="formulario-conta">
          <p className="mensagem">Pronto. Use a nova senha da próxima vez que entrar em outro aparelho.</p>
          <button type="button" className="botao botao-primario botao-grande" onClick={aoFechar}>
            Fechar
          </button>
        </div>
      ) : (
        <form className="formulario-conta" onSubmit={enviar} noValidate>
          <CampoSenha
            ref={refAtual}
            rotulo="Senha atual"
            autoComplete="current-password"
            value={valores.atual}
            onChange={(e) => alterar('atual', e.target.value)}
            erro={erros.atual}
          />
          <CampoSenha
            ref={refNova}
            rotulo="Nova senha"
            autoComplete="new-password"
            value={valores.nova}
            onChange={(e) => alterar('nova', e.target.value)}
            erro={erros.nova}
            dica={`Pelo menos ${SENHA_MIN} caracteres.`}
          />
          <CampoSenha
            ref={refConfirmacao}
            rotulo="Repita a nova senha"
            autoComplete="new-password"
            value={valores.confirmacao}
            onChange={(e) => alterar('confirmacao', e.target.value)}
            erro={erros.confirmacao}
          />
          {erroGeral && (
            <p className="formulario-conta-erro" role="alert">
              {erroGeral}
            </p>
          )}
          <button type="submit" className="botao botao-primario botao-grande" disabled={enviando} aria-busy={enviando}>
            {enviando ? 'salvando...' : 'Trocar senha'}
          </button>
        </form>
      )}
    </Dialogo>
  )
}
