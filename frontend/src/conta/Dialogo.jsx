import { useId } from 'react'
import { IconeLimpar } from '../icons'
import { useDialogoModal } from './useDialogoModal'

/** Modal nativo ({@code <dialog>}): foco preso, Esc fecha e o resto da página fica inerte. */
export default function Dialogo({ titulo, subtitulo, aoFechar, children, className = '' }) {
  const propsDialogo = useDialogoModal(aoFechar)
  const id = useId()

  return (
    <dialog
      {...propsDialogo}
      className={`dialogo ${className}`}
      aria-labelledby={`${id}-titulo`}
      aria-describedby={subtitulo ? `${id}-subtitulo` : undefined}
    >
      <div className="dialogo-caixa">
        <header className="dialogo-cabecalho">
          <div>
            <h2 id={`${id}-titulo`} className="dialogo-titulo">
              {titulo}
            </h2>
            {subtitulo && (
              <p id={`${id}-subtitulo`} className="dialogo-subtitulo">
                {subtitulo}
              </p>
            )}
          </div>
          <button type="button" className="dialogo-fechar" onClick={aoFechar} aria-label="Fechar">
            <IconeLimpar tamanho={14} />
          </button>
        </header>
        {children}
      </div>
    </dialog>
  )
}
