function Icone({ children, tamanho = 18, ...props }) {
  return (
    <svg
      width={tamanho}
      height={tamanho}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      {children}
    </svg>
  )
}

export function IconeBusca(props) {
  return (
    <Icone {...props}>
      <circle cx="11" cy="11" r="7" />
      <line x1="21" y1="21" x2="16.65" y2="16.65" />
    </Icone>
  )
}

export function IconePin(props) {
  return (
    <Icone {...props}>
      <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" />
      <circle cx="12" cy="10" r="3" />
    </Icone>
  )
}

export function IconeCalendario(props) {
  return (
    <Icone {...props}>
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <line x1="16" y1="2" x2="16" y2="6" />
      <line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </Icone>
  )
}

export function IconeSol(props) {
  return (
    <Icone {...props}>
      <circle cx="12" cy="12" r="4" />
      <line x1="12" y1="2" x2="12" y2="4" />
      <line x1="12" y1="20" x2="12" y2="22" />
      <line x1="4.2" y1="4.2" x2="5.6" y2="5.6" />
      <line x1="18.4" y1="18.4" x2="19.8" y2="19.8" />
      <line x1="2" y1="12" x2="4" y2="12" />
      <line x1="20" y1="12" x2="22" y2="12" />
      <line x1="4.2" y1="19.8" x2="5.6" y2="18.4" />
      <line x1="18.4" y1="5.6" x2="19.8" y2="4.2" />
    </Icone>
  )
}

export function IconeLua(props) {
  return (
    <Icone {...props}>
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z" />
    </Icone>
  )
}

export function IconeAlerta(props) {
  return (
    <Icone {...props}>
      <circle cx="12" cy="12" r="10" />
      <line x1="12" y1="8" x2="12" y2="13" />
      <line x1="12" y1="16.5" x2="12" y2="16.51" />
    </Icone>
  )
}

export function IconeCaixaVazia(props) {
  return (
    <Icone {...props}>
      <path d="M3 8.5 12 4l9 4.5-9 4.5-9-4.5Z" />
      <path d="M3 8.5V16l9 4.5 9-4.5V8.5" />
      <line x1="12" y1="13" x2="12" y2="20.5" />
    </Icone>
  )
}

export function IconeSetaExterna(props) {
  return (
    <Icone {...props}>
      <path d="M7 17 17 7" />
      <path d="M8 7h9v9" />
    </Icone>
  )
}

export function IconeLimpar(props) {
  return (
    <Icone {...props}>
      <line x1="18" y1="6" x2="6" y2="18" />
      <line x1="6" y1="6" x2="18" y2="18" />
    </Icone>
  )
}

export function IconeMarcador(props) {
  return (
    <Icone {...props}>
      <path d="M6 3h12v18l-6-4-6 4V3Z" />
    </Icone>
  )
}

export function IconeMarcadorCheio(props) {
  return (
    <Icone {...props}>
      <path d="M6 3h12v18l-6-4-6 4V3Z" fill="currentColor" />
    </Icone>
  )
}

export function IconeMais(props) {
  return (
    <Icone {...props}>
      <line x1="12" y1="5" x2="12" y2="19" />
      <line x1="5" y1="12" x2="19" y2="12" />
    </Icone>
  )
}

export function IconeDownload(props) {
  return (
    <Icone {...props}>
      <path d="M12 4v11" />
      <path d="m7 10 5 5 5-5" />
      <path d="M5 20h14" />
    </Icone>
  )
}

export function IconeRelogio(props) {
  return (
    <Icone {...props}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </Icone>
  )
}

export function IconeUsuario(props) {
  return (
    <Icone {...props}>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21a8 8 0 0 1 16 0" />
    </Icone>
  )
}

export function IconeAlca(props) {
  return (
    <Icone {...props}>
      <circle cx="9" cy="6" r="1" fill="currentColor" />
      <circle cx="15" cy="6" r="1" fill="currentColor" />
      <circle cx="9" cy="12" r="1" fill="currentColor" />
      <circle cx="15" cy="12" r="1" fill="currentColor" />
      <circle cx="9" cy="18" r="1" fill="currentColor" />
      <circle cx="15" cy="18" r="1" fill="currentColor" />
    </Icone>
  )
}

export function IconeQuadro(props) {
  return (
    <Icone {...props}>
      <rect x="3" y="4" width="5" height="16" rx="1" />
      <rect x="10" y="4" width="5" height="11" rx="1" />
      <rect x="17" y="4" width="4" height="7" rx="1" />
    </Icone>
  )
}

export function IconeLista(props) {
  return (
    <Icone {...props}>
      <line x1="9" y1="6" x2="21" y2="6" />
      <line x1="9" y1="12" x2="21" y2="12" />
      <line x1="9" y1="18" x2="21" y2="18" />
      <circle cx="4.5" cy="6" r="1" fill="currentColor" />
      <circle cx="4.5" cy="12" r="1" fill="currentColor" />
      <circle cx="4.5" cy="18" r="1" fill="currentColor" />
    </Icone>
  )
}

export function IconeLixeira(props) {
  return (
    <Icone {...props}>
      <path d="M4 7h16" />
      <path d="M9 7V4h6v3" />
      <path d="M6 7l1 13h10l1-13" />
    </Icone>
  )
}
