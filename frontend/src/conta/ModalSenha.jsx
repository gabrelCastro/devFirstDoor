import { useState } from 'react'
import Dialogo from './Dialogo'
import { chamarApi } from './sessao'
import './conta.css'

export default function ModalSenha({ aoFechar }) {
  const [atual, setAtual] = useState('')
  const [nova, setNova] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState(null)
  const [concluido, setConcluido] = useState(false)

  async function enviar(evento) {
    evento.preventDefault()
    if (nova.length < 8) {
      setErro('A nova senha precisa ter pelo menos 8 caracteres.')
      return
    }
    if (nova !== confirmacao) {
      setErro('As senhas não conferem.')
      return
    }
    setEnviando(true)
    setErro(null)
    try {
      await chamarApi('/api/conta/senha', { method: 'PUT', corpo: { senhaAtual: atual, novaSenha: nova } })
      setConcluido(true)
    } catch (e) {
      setErro(e.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Dialogo titulo="$ trocar senha" aoFechar={aoFechar}>
      {concluido ? (
        <div className="formulario">
          <p className="mensagem">Senha trocada. Os outros aparelhos em que você estava logado foram desconectados.</p>
          <button type="button" className="botao botao-primario" onClick={aoFechar} autoFocus>
            fechar
          </button>
        </div>
      ) : (
        <form className="formulario" onSubmit={enviar} noValidate>
          <label className="campo">
            <span>senha atual</span>
            <input
              type="password"
              autoComplete="current-password"
              value={atual}
              onChange={(e) => setAtual(e.target.value)}
              required
              autoFocus
            />
          </label>
          <label className="campo">
            <span>nova senha</span>
            <input type="password" autoComplete="new-password" value={nova} onChange={(e) => setNova(e.target.value)} required />
          </label>
          <label className="campo">
            <span>repita a nova senha</span>
            <input
              type="password"
              autoComplete="new-password"
              value={confirmacao}
              onChange={(e) => setConfirmacao(e.target.value)}
              required
            />
          </label>
          {erro && (
            <p className="mensagem mensagem-erro" role="alert">
              {erro}
            </p>
          )}
          <button type="submit" className="botao botao-primario" disabled={enviando} aria-busy={enviando}>
            {enviando ? 'salvando...' : 'trocar senha'}
          </button>
        </form>
      )}
    </Dialogo>
  )
}
