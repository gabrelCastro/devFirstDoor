import { useState } from 'react'
import { ErroSemPermissao, chamarAdmin } from './api'
import { entrar } from '../conta/sessao'

export default function Login({ aoEntrar, aviso }) {
  const [usuario, setUsuario] = useState('')
  const [senha, setSenha] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState(null)

  async function enviar(evento) {
    evento.preventDefault()
    setEnviando(true)
    setErro(null)
    try {
      await entrar(usuario, senha)
      // O login vale para qualquer conta; aqui só entra quem tem o papel ADMIN.
      const dados = await chamarAdmin('/me')
      setSenha('')
      aoEntrar(dados.usuario)
    } catch (e) {
      setErro(e instanceof ErroSemPermissao ? 'Esta conta não tem acesso à área administrativa.' : e.message)
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
