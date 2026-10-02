import { Suspense, lazy } from 'react'
import App from './App.jsx'
import { SessaoProvider } from './conta/SessaoContext.jsx'

// Sem roteador: cada caminho carrega sua tela sob demanda, para não pesar a página pública.
const Admin = lazy(() => import('./admin/Admin.jsx'))
const Candidaturas = lazy(() => import('./candidaturas/Candidaturas.jsx'))

function telaDoCaminho(caminho) {
  if (/^\/admin(\/|$)/.test(caminho)) return 'admin'
  if (/^\/candidaturas(\/|$)/.test(caminho)) return 'candidaturas'
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
      </Suspense>
    </SessaoProvider>
  )
}
