import { createContext, useContext } from 'react'

export const SessaoContexto = createContext(null)

/** Conta logada e ações de sessão (ver SessaoProvider). */
export function useSessao() {
  const contexto = useContext(SessaoContexto)
  if (!contexto) throw new Error('useSessao precisa estar dentro de <SessaoProvider>')
  return contexto
}
