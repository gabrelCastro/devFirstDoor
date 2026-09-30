import { useEffect, useState } from 'react'

/** Tema claro/escuro compartilhado pela página pública e pelo /admin. */
export function useTema() {
  const [tema, setTema] = useState(() => {
    try {
      const salvo = localStorage.getItem('tema')
      if (salvo === 'claro' || salvo === 'escuro') return salvo
    } catch {
      // ignora falha ao ler localStorage (modo privado, etc.)
    }
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'escuro' : 'claro'
  })

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', tema)
    try {
      localStorage.setItem('tema', tema)
    } catch {
      // ignora falha ao gravar no localStorage
    }
  }, [tema])

  return [tema, () => setTema((atual) => (atual === 'claro' ? 'escuro' : 'claro'))]
}
