import { useEffect, useRef, useState } from 'react'

const CAMPOS = '[data-autofocus], input:not([type=hidden]):not(:disabled), select:not(:disabled), textarea:not(:disabled)'

/**
 * Abre um <dialog> como modal ao montar e cuida do que o navegador não faz sozinho quando o
 * componente some da tela: o foco vai para o primeiro campo (o showModal escolheria o botão de
 * fechar) e, ao desmontar, volta para quem abriu o modal.
 *
 * Devolve as props do <dialog>. Fechar clicando no fundo só vale se o clique começou e terminou
 * no fundo: selecionar o texto de um campo e soltar o mouse fora não fecha mais o modal.
 */
export function useDialogoModal(aoFechar) {
  const ref = useRef(null)
  const inicioNoFundo = useRef(false)
  // Lido no primeiro render, antes de o modal existir: no efeito (que o StrictMode roda duas
  // vezes em dev) o foco já poderia estar dentro do próprio modal.
  const [anterior] = useState(() => document.activeElement)

  useEffect(() => {
    const dialogo = ref.current
    if (dialogo && !dialogo.open) dialogo.showModal()
    dialogo?.querySelector(CAMPOS)?.focus()
    return () => {
      if (anterior instanceof HTMLElement && anterior.isConnected) anterior.focus()
    }
  }, [anterior])

  return {
    ref,
    onCancel: (evento) => {
      evento.preventDefault()
      aoFechar()
    },
    onPointerDown: (evento) => {
      inicioNoFundo.current = evento.target === ref.current
    },
    onClick: (evento) => {
      if (inicioNoFundo.current && evento.target === ref.current) aoFechar()
      inicioNoFundo.current = false
    },
  }
}
