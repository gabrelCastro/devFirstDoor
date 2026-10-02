import { useEffect, useRef, useState } from 'react'
import { IconeUsuario } from '../icons'
import { useSessao } from './useSessao'
import ModalSenha from './ModalSenha'
import './conta.css'

/** "entrar" para visitantes; menu com candidaturas, senha, admin e sair para quem está logado. */
export default function MenuConta({ paginaAtual }) {
  const { usuario, verificando, pedirLogin, sair } = useSessao()
  const [aberto, setAberto] = useState(false)
  const [trocandoSenha, setTrocandoSenha] = useState(false)
  const raiz = useRef(null)

  useEffect(() => {
    if (!aberto) return
    function fecharFora(evento) {
      if (!raiz.current?.contains(evento.target)) setAberto(false)
    }
    function fecharEsc(evento) {
      if (evento.key === 'Escape') setAberto(false)
    }
    document.addEventListener('pointerdown', fecharFora)
    document.addEventListener('keydown', fecharEsc)
    return () => {
      document.removeEventListener('pointerdown', fecharFora)
      document.removeEventListener('keydown', fecharEsc)
    }
  }, [aberto])

  if (verificando) return <span className="menu-conta-carregando" aria-hidden="true" />

  if (!usuario) {
    return (
      <button type="button" className="menu-conta-botao" onClick={() => pedirLogin()}>
        <IconeUsuario tamanho={14} />
        entrar
      </button>
    )
  }

  async function encerrar() {
    setAberto(false)
    await sair()
    if (paginaAtual === 'candidaturas') window.location.assign('/')
  }

  return (
    <div className="menu-conta" ref={raiz}>
      <button
        type="button"
        className="menu-conta-botao"
        aria-haspopup="true"
        aria-expanded={aberto}
        onClick={() => setAberto((valor) => !valor)}
      >
        <IconeUsuario tamanho={14} />
        <span className="menu-conta-nome">{usuario.usuario}</span>
        <span aria-hidden="true">▾</span>
      </button>
      {aberto && (
        <div className="menu-conta-lista" role="menu">
          {paginaAtual !== 'candidaturas' && (
            <a role="menuitem" href="/candidaturas">
              minhas candidaturas
            </a>
          )}
          {paginaAtual !== 'vagas' && (
            <a role="menuitem" href="/">
              vagas
            </a>
          )}
          {usuario.papel === 'ADMIN' && (
            <a role="menuitem" href="/admin">
              área admin
            </a>
          )}
          <button
            type="button"
            role="menuitem"
            onClick={() => {
              setAberto(false)
              setTrocandoSenha(true)
            }}
          >
            trocar senha
          </button>
          <button type="button" role="menuitem" className="menu-conta-sair" onClick={encerrar}>
            sair
          </button>
        </div>
      )}
      {trocandoSenha && <ModalSenha aoFechar={() => setTrocandoSenha(false)} />}
    </div>
  )
}
