import { useState } from 'react'
import { ErroNaoAutenticado, chamarAdmin, montarCredencial, salvarCredencial } from './api'

export default function Login({ aoEntrar, aviso }) {
  const [usuario, setUsuario] = useState('')
  const [senha, setSenha] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState(null)

  async function enviar(evento) {
    evento.preventDefault()
    setEnviando(true)
    setErro(null)
    const credencial = montarCredencial(usuario, senha)
    try {
      const dados = await chamarAdmin('/me', { credencial })
      salvarCredencial(credencial)
      setSenha('')
      aoEntrar(dados.usuario)
    } catch (e) {
      setErro(
        e instanceof ErroNaoAutenticado
          ? 'Usuário ou senha incorretos (ou a área admin está desligada no servidor).'
          : e.message,
      )
    } finally {
      setEnviando(false)
    }
  }

  const mensagem = erro ?? aviso

  return (
    <form className="admin-login" onSubmit={enviar}>
      <p className="admin-login-titulo">$ login</p>

      <label className="admin-campo">
        <span>usuário</span>
        <input
          type="text"
          autoComplete="username"
          value={usuario}
          onChange={(e) => setUsuario(e.target.value)}
          required
          autoFocus
        />
      </label>

      <label className="admin-campo">
        <span>senha</span>
        <input
          type="password"
          autoComplete="current-password"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          required
        />
      </label>

      {mensagem && (
        <p className="admin-mensagem admin-mensagem-erro" role="alert">
          {mensagem}
        </p>
      )}

      <button type="submit" className="admin-botao admin-botao-primario" disabled={enviando} aria-busy={enviando}>
        {enviando ? 'entrando...' : 'entrar'}
      </button>
    </form>
  )
}
