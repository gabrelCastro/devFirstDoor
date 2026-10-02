import { Suspense, lazy } from 'react'
import App from './App.jsx'
import { SessaoProvider } from './conta/SessaoContext.jsx'

// Sem roteador: cada caminho carrega sua tela sob demanda, para não pesar a página pública.
const Admin = lazy(() => import('./admin/Admin.jsx'))
const Candidaturas = lazy(() => import('./candidaturas/Candidaturas.jsx'))
const Curriculo = lazy(() => import('./curriculo/Curriculo.jsx'))

function telaDoCaminho(caminho) {
  if (/^\/admin(\/|$)/.test(caminho)) return 'admin'
  if (/^\/candidaturas(\/|$)/.test(caminho)) return 'candidaturas'
  if (/^\/curriculo(\/|$)/.test(caminho)) return 'curriculo'
  return 'vagas'
}

export default function Raiz() {
  const tela = telaDoCaminho(window.location.pathname)
  return (
    <SessaoProvider>
      {tela === 'vagas' && <App />}
      <Suspense fallback={null}>
        {tela === 'admin' && <Admin />}
        {tela === 'candidaturas' && <Candidaturas />}
        {tela === 'curriculo' && <Curriculo />}
      </Suspense>
    </SessaoProvider>
  )
}
