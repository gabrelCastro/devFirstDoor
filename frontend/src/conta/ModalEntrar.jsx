import { useState } from 'react'
import Dialogo from './Dialogo'
import './conta.css'

const USUARIO_VALIDO = /^[A-Za-z0-9._-]{3,40}$/

export default function ModalEntrar({ motivo, abaInicial = 'entrar', entrar, cadastrar, aoConcluir, aoFechar }) {
  const [aba, setAba] = useState(abaInicial)
  const [usuario, setUsuario] = useState('')
  const [senha, setSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState(null)

  const criando = aba === 'criar'

  function trocarAba(nova) {
    setAba(nova)
    setErro(null)
  }

  async function enviar(evento) {
    evento.preventDefault()
    if (criando) {
      if (!USUARIO_VALIDO.test(usuario)) {
        setErro('Use de 3 a 40 caracteres: letras, números, ponto, hífen ou sublinhado.')
        return
      }
      if (senha.length < 8) {
        setErro('A senha precisa ter pelo menos 8 caracteres.')
        return
      }
      if (senha !== confirmacao) {
        setErro('As senhas não conferem.')
        return
      }
    }
    setEnviando(true)
    setErro(null)
    try {
      const conta = criando ? await cadastrar(usuario, senha) : await entrar(usuario, senha)
      aoConcluir(conta)
    } catch (e) {
      setErro(e.message)
      setEnviando(false)
    }
  }

  return (
    <Dialogo titulo={criando ? '$ criar conta' : '$ entrar'} aoFechar={aoFechar} className="dialogo-entrar">
      <div className="abas-grupo dialogo-abas" role="tablist" aria-label="Entrar ou criar conta">
        <button
          type="button"
          role="tab"
          aria-selected={!criando}
          className={`aba ${!criando ? 'aba-ativa' : ''}`}
          onClick={() => trocarAba('entrar')}
        >
          entrar
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={criando}
          className={`aba ${criando ? 'aba-ativa' : ''}`}
          onClick={() => trocarAba('criar')}
        >
          criar conta
        </button>
      </div>

      {motivo && <p className="dialogo-motivo">{motivo}</p>}

      <form className="formulario" onSubmit={enviar} noValidate>
        <label className="campo">
          <span>usuário</span>
          <input
            type="text"
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            value={usuario}
            onChange={(e) => setUsuario(e.target.value)}
            required
           
          />
        </label>
        <label className="campo">
          <span>senha</span>
          <input
            type="password"
            autoComplete={criando ? 'new-password' : 'current-password'}
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            required
          />
        </label>
        {criando && (
          <label className="campo">
            <span>repita a senha</span>
            <input
              type="password"
              autoComplete="new-password"
              value={confirmacao}
              onChange={(e) => setConfirmacao(e.target.value)}
              required
            />
          </label>
        )}

        {erro && (
          <p className="mensagem mensagem-erro" role="alert">
            {erro}
          </p>
        )}

        <button type="submit" className="botao botao-primario" disabled={enviando} aria-busy={enviando}>
          {enviando ? 'aguarde...' : criando ? 'criar conta e entrar' : 'entrar'}
        </button>
        {criando && (
          <p className="formulario-nota">
            Sua conta guarda só usuário e senha. As candidaturas que você acompanhar ficam visíveis só para você.
          </p>
        )}
      </form>
    </Dialogo>
  )
}
