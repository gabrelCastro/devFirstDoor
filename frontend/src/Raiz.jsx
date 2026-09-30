import { Suspense, lazy } from 'react'
import App from './App.jsx'

// Sem roteador: /admin carrega a área administrativa sob demanda, para não pesar a página pública.
const Admin = lazy(() => import('./admin/Admin.jsx'))

export default function Raiz() {
  const ehAdmin = /^\/admin(\/|$)/.test(window.location.pathname)
  if (!ehAdmin) return <App />
  return (
    <Suspense fallback={null}>
      <Admin />
    </Suspense>
  )
}
