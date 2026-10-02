import { useId, useState } from 'react'
import { IconeLimpar, IconeMais } from '../icons'
import { separarTermos } from './curriculoDados'

export function CampoTexto({ rotulo, valor, aoMudar, tipo = 'text', dica, maximo = 150, ...resto }) {
  const id = useId()
  return (
    <div className="campo-cv">
      <label htmlFor={id}>{rotulo}</label>
      <input id={id} type={tipo} value={valor} maxLength={maximo} onChange={(e) => aoMudar(e.target.value)} {...resto} />
      {dica && <p className="campo-cv-dica">{dica}</p>}
    </div>
  )
}

export function CampoArea({ rotulo, valor, aoMudar, maximo, linhas = 3, dica, ...resto }) {
  const id = useId()
  return (
    <div className="campo-cv">
      <label htmlFor={id}>{rotulo}</label>
      <textarea id={id} rows={linhas} value={valor} maxLength={maximo} onChange={(e) => aoMudar(e.target.value)} {...resto} />
      <p className="campo-cv-dica">
        {dica}
        {maximo && <span className="campo-cv-contador">{valor.length}/{maximo}</span>}
      </p>
    </div>
  )
}

/** Tecnologias/habilidades como chips: Enter ou vírgula adiciona; colar uma lista separa sozinho. */
export function CampoTermos({ rotulo, termos, aoMudar, sugestao, maximo = 50 }) {
  const id = useId()
  const [rascunho, setRascunho] = useState('')

  function adicionar(texto) {
    const novos = separarTermos([...termos, ...separarTermos(texto)].join(','))
    aoMudar(novos.slice(0, maximo))
    setRascunho('')
  }

  return (
    <div className="campo-cv">
      <label htmlFor={id}>{rotulo}</label>
      <div className="termos">
        {termos.map((termo) => (
          <span key={termo} className="termo">
            {termo}
            <button type="button" onClick={() => aoMudar(termos.filter((t) => t !== termo))} aria-label={`Remover ${termo}`}>
              <IconeLimpar tamanho={10} />
            </button>
          </span>
        ))}
        <input
          id={id}
          value={rascunho}
          placeholder={termos.length === 0 ? sugestao : 'adicionar...'}
          onChange={(e) => {
            const valor = e.target.value
            if (/[,;]/.test(valor)) adicionar(valor)
            else setRascunho(valor)
          }}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault()
              if (rascunho.trim()) adicionar(rascunho)
            } else if (e.key === 'Backspace' && !rascunho && termos.length) {
              aoMudar(termos.slice(0, -1))
            }
          }}
          onBlur={() => rascunho.trim() && adicionar(rascunho)}
        />
      </div>
      <p className="campo-cv-dica">Enter ou vírgula para adicionar.</p>
    </div>
  )
}

/** Tópicos de uma experiência/projeto: cada um um textarea, com reordenar e remover. */
export function ListaTopicos({ topicos, aoMudar, rotulo }) {
  function alterar(indice, texto) {
    aoMudar(topicos.map((t, i) => (i === indice ? { ...t, texto } : t)))
  }
  function mover(indice, delta) {
    const destino = indice + delta
    if (destino < 0 || destino >= topicos.length) return
    const nova = [...topicos]
    ;[nova[indice], nova[destino]] = [nova[destino], nova[indice]]
    aoMudar(nova)
  }
  return (
    <fieldset className="topicos">
      <legend>{rotulo}</legend>
      {topicos.map((topico, indice) => (
        <div key={topico.id || `novo-${indice}`} className="topico">
          <span className="topico-marcador" aria-hidden="true">•</span>
          <textarea
            rows={2}
            maxLength={400}
            value={topico.texto}
            aria-label={`Tópico ${indice + 1}`}
            placeholder="O que você fez? Ex.: Desenvolvi a API de cadastro em Java com Spring Boot"
            onChange={(e) => alterar(indice, e.target.value)}
          />
          <div className="topico-acoes">
            <button type="button" onClick={() => mover(indice, -1)} disabled={indice === 0} aria-label={`Subir tópico ${indice + 1}`}>↑</button>
            <button type="button" onClick={() => mover(indice, 1)} disabled={indice === topicos.length - 1} aria-label={`Descer tópico ${indice + 1}`}>↓</button>
            <button type="button" onClick={() => aoMudar(topicos.filter((_, i) => i !== indice))} aria-label={`Remover tópico ${indice + 1}`}>
              <IconeLimpar tamanho={11} />
            </button>
          </div>
        </div>
      ))}
      {topicos.length < 8 && (
        <button type="button" className="botao-adicionar" onClick={() => aoMudar([...topicos, { id: '', texto: '' }])}>
          <IconeMais tamanho={11} /> tópico
        </button>
      )}
    </fieldset>
  )
}

/** Cartão de um item de lista (experiência, projeto...): título, reordenar e remover. */
export function CartaoItem({ titulo, indice, total, aoMover, aoRemover, children }) {
  return (
    <article className="item-cv">
      <header className="item-cv-cabecalho">
        <h4>{titulo}</h4>
        <div className="item-cv-acoes">
          <button type="button" onClick={() => aoMover(-1)} disabled={indice === 0} aria-label={`Subir ${titulo}`}>↑</button>
          <button type="button" onClick={() => aoMover(1)} disabled={indice === total - 1} aria-label={`Descer ${titulo}`}>↓</button>
          <button type="button" className="item-cv-remover" onClick={aoRemover}>remover</button>
        </div>
      </header>
      <div className="item-cv-campos">{children}</div>
    </article>
  )
}
