import { IconeLimpar } from '../icons'
import { useDialogoModal } from './useDialogoModal'

/** Modal nativo ({@code <dialog>}): foco preso, Esc fecha e o resto da página fica inerte. */
export default function Dialogo({ titulo, aoFechar, children, className = '' }) {
  const propsDialogo = useDialogoModal(aoFechar)

  return (
    <dialog {...propsDialogo} className={`dialogo ${className}`} aria-label={titulo}>
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
