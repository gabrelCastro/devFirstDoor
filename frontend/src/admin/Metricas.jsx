import { useEffect, useMemo, useState } from 'react'
import { ErroNaoAutenticado, chamarAdmin } from './api'
import { MOTIVO_DESCARTE_LABEL, prepararSerieNovas, somarValores } from './adminDados'

const NIVEL_LABEL = { ESTAGIO: 'estágio', JUNIOR: 'júnior' }
const MODALIDADE_LABEL = { REMOTA: 'remotas', NAO_REMOTA: 'não remotas' }

function formatarNumero(valor, casas = 0) {
  return Number(valor ?? 0).toLocaleString('pt-BR', { maximumFractionDigits: casas })
}

function GraficoBarras({ dados, rotulos = {} }) {
  const entradas = Object.entries(dados ?? {})
  const maximo = Math.max(1, ...entradas.map(([, valor]) => valor))
  if (entradas.length === 0) return <p className="admin-fraco">sem dados no período</p>
  return (
    <ul className="grafico-barras">
      {entradas.map(([chave, valor]) => (
        <li key={chave} aria-label={`${rotulos[chave] ?? chave}: ${valor}`}>
          <span className="grafico-rotulo">{rotulos[chave] ?? chave}</span>
          <span className="grafico-trilho" aria-hidden="true">
            <span className="grafico-barra" style={{ width: `${(valor / maximo) * 100}%` }} />
          </span>
          <strong>{formatarNumero(valor)}</strong>
        </li>
      ))}
    </ul>
  )
}

function GraficoNovas({ registros }) {
  const serie = useMemo(() => prepararSerieNovas(registros), [registros])
  const maximo = Math.max(1, ...serie.map((item) => item.total))
  const pontos = serie.map((item, indice) => {
    const x = 20 + (indice / (serie.length - 1)) * 560
    const y = 150 - (item.total / maximo) * 120
    return `${x},${y}`
  }).join(' ')
  const total = serie.reduce((soma, item) => soma + item.total, 0)
  const formatarData = (data) => new Date(`${data}T12:00:00Z`).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', timeZone: 'UTC' })

  return (
    <div className="grafico-linha">
      <svg viewBox="0 0 600 180" role="img" aria-label={`${total} vagas novas nos últimos 30 dias`}>
        <line x1="20" y1="150" x2="580" y2="150" className="grafico-eixo" />
        <polygon points={`20,150 ${pontos} 580,150`} className="grafico-area" />
        <polyline points={pontos} className="grafico-serie" />
      </svg>
      <div className="grafico-datas"><span>{formatarData(serie[0].data)}</span><span>{formatarData(serie.at(-1).data)}</span></div>
    </div>
  )
}

function CartaoNumero({ rotulo, valor, detalhe }) {
  return (
    <div className="metrica-numero">
      <span>{rotulo}</span>
      <strong>{valor}</strong>
      {detalhe && <small>{detalhe}</small>}
    </div>
  )
}

export default function Metricas({ aoExpirar }) {
  const [metricas, setMetricas] = useState(null)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    let cancelado = false
    chamarAdmin('/metricas')
      .then((dados) => { if (!cancelado) setMetricas(dados) })
      .catch((e) => {
        if (cancelado) return
        if (e instanceof ErroNaoAutenticado) aoExpirar()
        else setErro(e.message)
      })
    return () => { cancelado = true }
  }, [aoExpirar])

  if (erro) return <p className="admin-mensagem admin-mensagem-erro" role="alert">{erro}</p>
  if (!metricas) return <p className="admin-carregando">calculando métricas...</p>

  const totalAtivas = somarValores(metricas.vagasAtivasPorFonte)
  const totalExclusivas = somarValores(metricas.vagasExclusivasPorFonte)
  const novas30Dias = (metricas.vagasNovasUltimos30Dias ?? []).reduce((total, item) => total + item.total, 0)
  return (
    <section aria-labelledby="metricas-titulo">
      <div className="admin-barra">
        <div>
          <h2 id="metricas-titulo" className="admin-secao-titulo">métricas</h2>
          <p className="admin-fraco">Visão consolidada das vagas e dos filtros.</p>
        </div>
      </div>

      <div className="metricas-numeros">
        <CartaoNumero rotulo="vagas ativas" valor={formatarNumero(totalAtivas)} />
        <CartaoNumero rotulo="novas em 30 dias" valor={formatarNumero(novas30Dias)} />
        <CartaoNumero rotulo="exclusivas" valor={formatarNumero(totalExclusivas)} detalhe="sem equivalente em outra fonte" />
        <CartaoNumero rotulo="tempo médio no ar" valor={`${formatarNumero(metricas.tempoMedioNoArHoras / 24, 1)} dias`} />
      </div>

      <div className="metricas-grade">
        <section className="metrica-grafico metrica-grafico-largo">
          <h3>vagas novas · últimos 30 dias</h3>
          <GraficoNovas registros={metricas.vagasNovasUltimos30Dias} />
        </section>
        <section className="metrica-grafico"><h3>ativas por fonte</h3><GraficoBarras dados={metricas.vagasAtivasPorFonte} /></section>
        <section className="metrica-grafico"><h3>exclusivas por fonte</h3><GraficoBarras dados={metricas.vagasExclusivasPorFonte} /></section>
        <section className="metrica-grafico"><h3>ativas por nível</h3><GraficoBarras dados={metricas.vagasAtivasPorNivel} rotulos={NIVEL_LABEL} /></section>
        <section className="metrica-grafico"><h3>ativas por modalidade</h3><GraficoBarras dados={metricas.vagasAtivasPorModalidade} rotulos={MODALIDADE_LABEL} /></section>
        <section className="metrica-grafico metrica-grafico-largo"><h3>descartes por motivo · últimos 7 dias</h3><GraficoBarras dados={metricas.descartesPorMotivoUltimos7Dias} rotulos={MOTIVO_DESCARTE_LABEL} /></section>
      </div>
    </section>
  )
}
