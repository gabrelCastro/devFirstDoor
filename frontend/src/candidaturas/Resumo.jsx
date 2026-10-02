import { useState } from 'react'
import { formatarDiaCurto, formatarMediaDias, formatarPercentual } from './candidaturasDados'

function Indicador({ valor, rotulo, detalhe, destaque }) {
  return (
    <div className="indicador" data-destaque={destaque || undefined}>
      <strong className="indicador-valor">{valor}</strong>
      <span className="indicador-rotulo">{rotulo}</span>
      {detalhe && <span className="indicador-detalhe">{detalhe}</span>}
    </div>
  )
}

/**
 * Envios por semana: uma série só (sem legenda), barras finas apoiadas na base, com a contagem
 * da barra apontada no topo. A lista sr-only repete os números para leitores de tela.
 */
function GraficoSemanas({ semanas }) {
  const [ativa, setAtiva] = useState(null)
  const maximo = Math.max(1, ...semanas.map((s) => s.enviadas))
  const largura = 100 / semanas.length
  const atual = ativa ?? semanas.length - 1
  const semana = semanas[atual]

  return (
    <figure className="grafico-semanas">
      <figcaption>
        <span>envios por semana</span>
        <span className="grafico-semanas-foco" aria-live="polite">
          {semana ? `${semana.enviadas} · semana de ${formatarDiaCurto(semana.inicio)}` : ''}
        </span>
      </figcaption>
      <div className="grafico-semanas-area" onPointerLeave={() => setAtiva(null)}>
        {semanas.map((s, indice) => (
          <div
            key={s.inicio}
            className="grafico-semanas-coluna"
            style={{ width: `${largura}%` }}
            data-ativa={indice === atual || undefined}
            onPointerEnter={() => setAtiva(indice)}
            aria-hidden="true"
          >
            <span
              className="grafico-semanas-barra"
              style={{ height: s.enviadas === 0 ? '2px' : `${Math.max(8, (s.enviadas / maximo) * 100)}%` }}
              data-vazia={s.enviadas === 0 || undefined}
            />
          </div>
        ))}
      </div>
      <div className="grafico-semanas-eixo" aria-hidden="true">
        <span>{formatarDiaCurto(semanas[0]?.inicio)}</span>
        <span>esta semana</span>
      </div>
      <ul className="sr-only">
        {semanas.map((s) => (
          <li key={s.inicio}>
            Semana de {formatarDiaCurto(s.inicio)}: {s.enviadas} {s.enviadas === 1 ? 'envio' : 'envios'}
          </li>
        ))}
      </ul>
    </figure>
  )
}

export default function Resumo({ resumo }) {
  if (!resumo) {
    return <div className="resumo resumo-carregando" aria-hidden="true" />
  }
  return (
    <section className="resumo" aria-label="Resumo das candidaturas">
      <div className="indicadores">
        <Indicador
          valor={resumo.ativas}
          rotulo="em andamento"
          detalhe={resumo.encerradas > 0 ? `${resumo.encerradas} ${resumo.encerradas === 1 ? 'encerrada' : 'encerradas'}` : null}
        />
        <Indicador valor={resumo.enviadasNoMes} rotulo="enviadas no mês" detalhe={`${resumo.enviadas} no total`} />
        <Indicador
          valor={formatarPercentual(resumo.taxaResposta)}
          rotulo="taxa de resposta"
          detalhe={resumo.enviadas > 0 ? `${resumo.responderam} de ${resumo.enviadas} responderam` : 'sem envios ainda'}
        />
        <Indicador valor={formatarMediaDias(resumo.diasMedioAteResposta)} rotulo="até a 1ª resposta" detalhe="em média" />
        <Indicador
          valor={resumo.followUps}
          rotulo={resumo.followUps === 1 ? 'pede atenção' : 'pedem atenção'}
          detalhe="paradas ou com passo vencido"
          destaque={resumo.followUps > 0}
        />
      </div>
      <GraficoSemanas semanas={resumo.semanas} />
    </section>
  )
}
