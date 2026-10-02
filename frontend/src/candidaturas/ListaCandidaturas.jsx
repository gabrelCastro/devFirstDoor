import { ETAPAS, agruparPorEtapa, diasDesde, formatarDias } from './candidaturasDados'
import { Sinais } from './CartaoCandidatura'

/** Lista agrupada por etapa. É a visão do celular e a alternativa acessível ao arrastar. */
export default function ListaCandidaturas({ candidaturas, ocupada, aoAbrir, aoMover }) {
  const grupos = agruparPorEtapa(candidaturas)
  return (
    <div className="lista">
      {ETAPAS.filter((etapa) => grupos[etapa.id].length > 0).map((etapa) => (
        <section key={etapa.id} className="lista-grupo" data-etapa={etapa.id}>
          <h2 className="lista-grupo-titulo">
            <span className="chip-etapa" data-etapa={etapa.id}>{etapa.rotulo}</span>
            <span className="coluna-contagem">{grupos[etapa.id].length}</span>
          </h2>
          <ul>
            {grupos[etapa.id].map((c) => (
              <li key={c.id} className="lista-item">
                <button type="button" className="lista-item-abrir" onClick={() => aoAbrir(c.id)}>
                  <span className="cartao-empresa">{c.empresa}</span>
                  <span className="lista-item-titulo">{c.titulo}</span>
                  <span className="cartao-meta">
                    {c.externa ? 'externa' : c.fonte?.toLowerCase()} · {formatarDias(diasDesde(c.etapaDesde))}
                  </span>
                </button>
                <div className="lista-item-lado">
                  <div className="cartao-sinais">
                    <Sinais candidatura={c} />
                  </div>
                  <label className="sr-only" htmlFor={`mover-${c.id}`}>
                    Etapa de {c.titulo}
                  </label>
                  <select
                    id={`mover-${c.id}`}
                    className="seletor-etapa"
                    data-etapa={c.etapa}
                    value={c.etapa}
                    disabled={ocupada === c.id}
                    aria-busy={ocupada === c.id}
                    onChange={(e) => aoMover(c, e.target.value)}
                  >
                    {ETAPAS.map((opcao) => (
                      <option key={opcao.id} value={opcao.id}>
                        {opcao.rotulo}
                      </option>
                    ))}
                  </select>
                </div>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  )
}
