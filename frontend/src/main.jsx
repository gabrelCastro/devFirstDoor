import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import Raiz from './Raiz.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Raiz />
  </StrictMode>,
)
