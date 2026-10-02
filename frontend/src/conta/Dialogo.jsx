import { useEffect, useRef } from 'react'
import { IconeLimpar } from '../icons'

/**
 * Modal nativo ({@code <dialog>}): foco preso, Esc fecha e o resto da página fica inerte.
 * Clicar fora (no fundo) também fecha.
 */
export default function Dialogo({ titulo, aoFechar, children, className = '' }) {
  const ref = useRef(null)

  useEffect(() => {
    const dialogo = ref.current
    if (dialogo && !dialogo.open) dialogo.showModal()
  }, [])

  function aoClicar(evento) {
    if (evento.target === ref.current) aoFechar()
  }

  return (
    <dialog
      ref={ref}
      className={`dialogo ${className}`}
      aria-label={titulo}
      onCancel={(evento) => {
        evento.preventDefault()
        aoFechar()
      }}
      onClick={aoClicar}
    >
      <div className="dialogo-caixa">
        <header className="dialogo-cabecalho">
          <p className="dialogo-titulo">{titulo}</p>
          <button type="button" className="dialogo-fechar" onClick={aoFechar} aria-label="Fechar">
            <IconeLimpar tamanho={14} />
          </button>
        </header>
        {children}
      </div>
    </dialog>
  )
}
