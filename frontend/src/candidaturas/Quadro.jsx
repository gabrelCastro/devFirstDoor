import { useState } from 'react'
import { IconeMais } from '../icons'
import CartaoCandidatura from './CartaoCandidatura'
import { ETAPAS_ATIVAS, ETAPAS_ENCERRADAS, agruparPorEtapa } from './candidaturasDados'

/**
 * Colunas das etapas ativas com arrastar e soltar (HTML5). Mudar de etapa também dá pelo
 * painel de detalhes, que é o caminho por teclado. Encerradas ficam numa faixa recolhível.
 */
export default function Quadro({ candidaturas, aoAbrir, aoMover, aoNovaExterna }) {
  const grupos = agruparPorEtapa(candidaturas)
  const [arrastando, setArrastando] = useState(null)
  const [alvo, setAlvo] = useState(null)
  const [mostrarEncerradas, setMostrarEncerradas] = useState(false)
  const encerradas = ETAPAS_ENCERRADAS.flatMap((etapa) => grupos[etapa.id])

  function soltar(evento, etapa) {
    evento.preventDefault()
    const id = Number(evento.dataTransfer.getData('text/plain'))
    setAlvo(null)
    setArrastando(null)
    const candidatura = candidaturas.find((c) => c.id === id)
    if (candidatura && candidatura.etapa !== etapa) aoMover(candidatura, etapa)
  }

  return (
    <>
      <div className="quadro" data-arrastando={arrastando != null || undefined}>
        {ETAPAS_ATIVAS.map((etapa) => {
          const itens = grupos[etapa.id]
          return (
            <section
              key={etapa.id}
              className="coluna"
              data-etapa={etapa.id}
              data-alvo={alvo === etapa.id || undefined}
              aria-label={`${etapa.rotulo}: ${itens.length}`}
              onDragOver={(evento) => {
                if (arrastando == null) return
                evento.preventDefault()
                evento.dataTransfer.dropEffect = 'move'
                if (alvo !== etapa.id) setAlvo(etapa.id)
              }}
              onDragLeave={(evento) => {
                if (!evento.currentTarget.contains(evento.relatedTarget)) setAlvo((atual) => (atual === etapa.id ? null : atual))
              }}
              onDrop={(evento) => soltar(evento, etapa.id)}
            >
              <header className="coluna-cabecalho">
                <h2>
                  <span className="coluna-prompt" aria-hidden="true">$</span> {etapa.rotulo}
                </h2>
                <span className="coluna-contagem">{itens.length}</span>
              </header>
              <p className="coluna-descricao">{etapa.descricao}</p>
              <div className="coluna-itens">
                {itens.map((candidatura) => (
                  <CartaoCandidatura
                    key={candidatura.id}
                    candidatura={candidatura}
                    arrastando={arrastando === candidatura.id}
                    aoAbrir={aoAbrir}
                    aoIniciarArraste={setArrastando}
                    aoTerminarArraste={() => {
                      setArrastando(null)
                      setAlvo(null)
                    }}
                  />
                ))}
                {itens.length === 0 && (
                  <p className="coluna-vazia">{arrastando != null ? 'solte aqui' : 'arraste um cartão para cá'}</p>
                )}
              </div>
              {etapa.id === 'INTERESSE' && (
                <button type="button" className="coluna-adicionar" onClick={aoNovaExterna}>
                  <IconeMais tamanho={12} /> vaga de fora do site
                </button>
              )}
            </section>
          )
        })}
      </div>

      {encerradas.length > 0 && (
        <section className="encerradas" aria-label="Candidaturas encerradas">
          <button
            type="button"
            className="encerradas-alternar"
            aria-expanded={mostrarEncerradas}
            onClick={() => setMostrarEncerradas((valor) => !valor)}
          >
            <span aria-hidden="true">{mostrarEncerradas ? '▾' : '▸'}</span> encerradas ({encerradas.length})
            <span className="encerradas-resumo">
              {ETAPAS_ENCERRADAS.filter((etapa) => grupos[etapa.id].length > 0)
                .map((etapa) => `${grupos[etapa.id].length} ${etapa.rotulo}`)
                .join(' · ')}
            </span>
          </button>
          {mostrarEncerradas && (
            <div className="encerradas-grade">
              {ETAPAS_ENCERRADAS.filter((etapa) => grupos[etapa.id].length > 0).map((etapa) => (
                <div key={etapa.id} className="encerradas-grupo" data-etapa={etapa.id}>
                  <h3 className="chip-etapa" data-etapa={etapa.id}>{etapa.rotulo}</h3>
                  <ul>
                    {grupos[etapa.id].map((c) => (
                      <li key={c.id}>
                        <button type="button" onClick={() => aoAbrir(c.id)}>
                          <span className="encerradas-empresa">{c.empresa}</span>
                          <span>{c.titulo}</span>
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          )}
        </section>
      )}
    </>
  )
}
