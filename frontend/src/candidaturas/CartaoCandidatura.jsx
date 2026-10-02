import { IconeAlca, IconeRelogio } from '../icons'
import {
  DIAS_PARA_FOLLOW_UP,
  diasDesde,
  formatarDiaCurto,
  formatarDias,
  precisaFollowUp,
  situacaoProximoPasso,
  vagaSaiuDoAr,
} from './candidaturasDados'

/** Selos de atenção comuns ao cartão do quadro e à linha da lista. */
export function Sinais({ candidatura }) {
  const situacao = situacaoProximoPasso(candidatura)
  const dias = diasDesde(candidatura.etapaDesde)
  const parada = candidatura.etapa === 'CANDIDATADO' && dias >= DIAS_PARA_FOLLOW_UP
  return (
    <>
      {candidatura.proximoPasso && (
        <span className="sinal sinal-passo" data-situacao={situacao ?? undefined} title={candidatura.proximoPasso}>
          <IconeRelogio tamanho={11} />
          {candidatura.dataProximoPasso ? formatarDiaCurto(candidatura.dataProximoPasso) : 'próximo passo'}
          {situacao === 'vencido' && ' · atrasado'}
          {situacao === 'hoje' && ' · hoje'}
        </span>
      )}
      {parada && <span className="sinal sinal-alerta">sem retorno há {dias}d</span>}
      {vagaSaiuDoAr(candidatura) && <span className="sinal">saiu do ar</span>}
    </>
  )
}

export default function CartaoCandidatura({ candidatura, arrastando, aoAbrir, aoIniciarArraste, aoTerminarArraste }) {
  const atencao = precisaFollowUp(candidatura)
  return (
    <article
      className="cartao"
      data-arrastando={arrastando || undefined}
      data-atencao={atencao || undefined}
      draggable
      onDragStart={(evento) => {
        evento.dataTransfer.effectAllowed = 'move'
        evento.dataTransfer.setData('text/plain', String(candidatura.id))
        aoIniciarArraste(candidatura.id)
      }}
      onDragEnd={aoTerminarArraste}
    >
      <span className="cartao-alca" aria-hidden="true">
        <IconeAlca tamanho={14} />
      </span>
      <p className="cartao-empresa">{candidatura.empresa}</p>
      <h3 className="cartao-titulo">
        {/* O botão cobre o cartão: clicar em qualquer ponto abre os detalhes. */}
        <button type="button" onClick={() => aoAbrir(candidatura.id)}>
          {candidatura.titulo}
        </button>
      </h3>
      <p className="cartao-meta">
        <span>{candidatura.externa ? 'externa' : candidatura.fonte?.toLowerCase()}</span>
        <span aria-hidden="true">·</span>
        <span title="Tempo nesta etapa">{formatarDias(diasDesde(candidatura.etapaDesde))}</span>
      </p>
      <div className="cartao-sinais">
        <Sinais candidatura={candidatura} />
      </div>
    </article>
  )
}
