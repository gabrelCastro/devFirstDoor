import { useEffect, useState } from 'react'
import { IconeLimpar, IconeLixeira, IconeSetaExterna } from '../icons'
import { ErroNaoAutenticado } from '../conta/sessao'
import { useDialogoModal } from '../conta/useDialogoModal'
import {
  adicionarNota,
  apagarNota,
  atualizarCandidatura,
  definirProximoPasso,
  detalharCandidatura,
  excluirCandidatura,
} from './api'
import {
  ETAPAS_ATIVAS,
  ETAPAS_ENCERRADAS,
  ROTULO_ETAPA,
  diasDesde,
  ehEncerrada,
  formatarDias,
  paraData,
  vagaSaiuDoAr,
} from './candidaturasDados'

function hojeIso() {
  const agora = new Date()
  return new Date(agora.getTime() - agora.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}

function formatarMomento(iso) {
  const data = paraData(iso)
  if (!data) return '—'
  return data.toLocaleString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function textoDoEvento(evento) {
  if (evento.tipo === 'CRIADA') return `adicionada em ${ROTULO_ETAPA[evento.etapaNova] ?? evento.etapaNova}`
  if (evento.tipo === 'ETAPA') {
    return `${ROTULO_ETAPA[evento.etapaAnterior] ?? evento.etapaAnterior} → ${ROTULO_ETAPA[evento.etapaNova] ?? evento.etapaNova}`
  }
  return evento.texto
}

/** Linha de etapas: as ativas viram um trilho clicável; encerrar pede o resultado. */
function Etapas({ candidatura, ocupado, aoMudar }) {
  const encerrada = ehEncerrada(candidatura.etapa)
  const indiceAtual = ETAPAS_ATIVAS.findIndex((e) => e.id === candidatura.etapa)
  const [encerrando, setEncerrando] = useState(false)

  return (
    <section className="painel-secao">
      <h3 className="painel-secao-titulo">etapa</h3>
      <ol className="trilho" aria-label="Etapas do processo">
        {ETAPAS_ATIVAS.map((etapa, indice) => {
          const estado = encerrada ? 'livre' : indice < indiceAtual ? 'feita' : indice === indiceAtual ? 'atual' : 'livre'
          return (
            <li key={etapa.id} data-estado={estado} data-etapa={etapa.id}>
              <button
                type="button"
                disabled={ocupado || estado === 'atual'}
                aria-current={estado === 'atual' ? 'step' : undefined}
                onClick={() => aoMudar(etapa.id)}
                title={`Mover para ${etapa.rotulo}`}
              >
                <span className="trilho-ponto" aria-hidden="true" />
                <span className="trilho-rotulo">{etapa.rotulo}</span>
              </button>
            </li>
          )
        })}
      </ol>

      {encerrada ? (
        <p className="painel-encerrada">
          Encerrada como <span className="chip-etapa" data-etapa={candidatura.etapa}>{ROTULO_ETAPA[candidatura.etapa]}</span>
          <span className="painel-fraco"> · clique numa etapa acima para reabrir</span>
        </p>
      ) : encerrando ? (
        <div className="painel-encerrar">
          <p className="painel-fraco">Como terminou?</p>
          <div className="painel-encerrar-opcoes">
            {ETAPAS_ENCERRADAS.map((etapa) => (
              <button
                key={etapa.id}
                type="button"
                className="chip-etapa painel-encerrar-opcao"
                data-etapa={etapa.id}
                disabled={ocupado}
                onClick={() => {
                  setEncerrando(false)
                  aoMudar(etapa.id)
                }}
              >
                {etapa.rotulo}
              </button>
            ))}
            <button type="button" className="botao-limpar" onClick={() => setEncerrando(false)}>
              cancelar
            </button>
          </div>
        </div>
      ) : (
        <button type="button" className="botao-limpar painel-botao-encerrar" onClick={() => setEncerrando(true)}>
          encerrar processo…
        </button>
      )}
      <p className="painel-fraco">
        nesta etapa {formatarDias(diasDesde(candidatura.etapaDesde))}
      </p>
    </section>
  )
}

function ProximoPasso({ candidatura, ocupado, aoSalvar }) {
  const [texto, setTexto] = useState(candidatura.proximoPasso ?? '')
  const [data, setData] = useState(candidatura.dataProximoPasso ?? '')
  const [erro, setErro] = useState(null)
  const alterado = texto !== (candidatura.proximoPasso ?? '') || data !== (candidatura.dataProximoPasso ?? '')

  function salvar(evento) {
    evento.preventDefault()
    // Data sem descrição seria descartada pelo servidor sem aviso.
    if (!texto.trim() && data) {
      setErro('Descreva o próximo passo para guardar a data.')
      return
    }
    setErro(null)
    aoSalvar(texto.trim() || null, texto.trim() ? data || null : null)
  }

  return (
    <section className="painel-secao">
      <h3 className="painel-secao-titulo">próximo passo</h3>
      <form className="painel-linha-form" autoComplete="off" onSubmit={salvar} noValidate>
        <label className="campo painel-campo-largo">
          <span className="sr-only">O que fazer</span>
          <input
            type="text"
            maxLength={200}
            placeholder="ex.: entregar o teste, mandar follow-up"
            value={texto}
            aria-invalid={erro ? true : undefined}
            onChange={(e) => {
              setTexto(e.target.value)
              setErro(null)
            }}
          />
        </label>
        <label className="campo">
          <span className="sr-only">Até quando</span>
          <input type="date" value={data} onChange={(e) => setData(e.target.value)} />
        </label>
        {erro && (
          <p className="painel-erro-campo painel-linha-acoes" role="alert">
            {erro}
          </p>
        )}
        <div className="painel-linha-acoes">
          <button type="submit" className="botao" disabled={ocupado || !alterado}>
            salvar
          </button>
          {candidatura.proximoPasso && (
            <button
              type="button"
              className="botao-limpar"
              disabled={ocupado}
              onClick={() => {
                setTexto('')
                setData('')
                setErro(null)
                aoSalvar(null, null)
              }}
            >
              concluído
            </button>
          )}
        </div>
      </form>
    </section>
  )
}

function DadosExterna({ candidatura, ocupado, aoSalvar }) {
  const [dados, setDados] = useState({
    titulo: candidatura.titulo,
    empresa: candidatura.empresa,
    local: candidatura.local ?? '',
    link: candidatura.link ?? '',
  })
  const alterado =
    dados.titulo !== candidatura.titulo ||
    dados.empresa !== candidatura.empresa ||
    dados.local !== (candidatura.local ?? '') ||
    dados.link !== (candidatura.link ?? '')

  return (
    <section className="painel-secao">
      <h3 className="painel-secao-titulo">dados da vaga</h3>
      <form
        className="painel-grade-form"
        autoComplete="off"
        onSubmit={(evento) => {
          evento.preventDefault()
          aoSalvar(dados)
        }}
      >
        {[
          ['titulo', 'vaga', 'text'],
          ['empresa', 'empresa', 'text'],
          ['local', 'local', 'text'],
          ['link', 'link', 'url'],
        ].map(([campo, rotulo, tipo]) => (
          <label key={campo} className="campo">
            <span>{rotulo}</span>
            <input
              type={tipo}
              value={dados[campo]}
              maxLength={campo === 'link' ? 1024 : 255}
              onChange={(e) => setDados((atual) => ({ ...atual, [campo]: e.target.value }))}
            />
          </label>
        ))}
        <div className="painel-linha-acoes">
          <button type="submit" className="botao" disabled={ocupado || !alterado}>
            salvar dados
          </button>
        </div>
      </form>
    </section>
  )
}

function LinhaDoTempo({ eventos, ocupado, aoAdicionar, aoApagar }) {
  const [nota, setNota] = useState('')
  const recentes = [...eventos].reverse()

  return (
    <section className="painel-secao">
      <h3 className="painel-secao-titulo">linha do tempo</h3>
      <form
        className="painel-nota-form"
        onSubmit={async (evento) => {
          evento.preventDefault()
          if (!nota.trim()) return
          if (await aoAdicionar(nota.trim())) setNota('')
        }}
      >
        <label className="campo">
          <span className="sr-only">Nova anotação</span>
          <textarea
            rows={2}
            maxLength={2000}
            placeholder="anote o que aconteceu: contato, perguntas da entrevista, feedback..."
            value={nota}
            onChange={(e) => setNota(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) e.currentTarget.form.requestSubmit()
            }}
          />
        </label>
        <button type="submit" className="botao" disabled={ocupado || !nota.trim()}>
          anotar
        </button>
      </form>
      <ol className="linha-tempo">
        {recentes.map((evento) => (
          <li key={evento.id} data-tipo={evento.tipo} data-etapa={evento.etapaNova ?? undefined}>
            <span className="linha-tempo-ponto" aria-hidden="true" />
            <div className="linha-tempo-corpo">
              <time dateTime={evento.data}>{formatarMomento(evento.data)}</time>
              <p className={evento.tipo === 'NOTA' ? 'linha-tempo-nota' : 'linha-tempo-etapa'}>{textoDoEvento(evento)}</p>
            </div>
            {evento.tipo === 'NOTA' && (
              <button
                type="button"
                className="linha-tempo-apagar"
                disabled={ocupado}
                onClick={() => aoApagar(evento.id)}
                aria-label="Apagar anotação"
                title="Apagar anotação"
              >
                <IconeLixeira tamanho={13} />
              </button>
            )}
          </li>
        ))}
      </ol>
    </section>
  )
}

/**
 * Detalhes de uma candidatura, numa folha lateral ({@code <dialog>}). Cada alteração vai direto
 * para a API e devolve a candidatura atualizada para o quadro.
 */
export default function PainelCandidatura({ id, versao, aoFechar, aoAtualizar, aoExcluir, aoExpirar, aoNaoEncontrada }) {
  const [detalhe, setDetalhe] = useState(null)
  const [erro, setErro] = useState(null)
  const [ocupado, setOcupado] = useState(false)
  const propsDialogo = useDialogoModal(aoFechar)

  useEffect(() => {
    let cancelado = false
    detalharCandidatura(id)
      .then((dados) => {
        if (!cancelado) {
          setDetalhe(dados)
          setErro(null)
        }
      })
      .catch((e) => {
        if (cancelado) return
        if (e instanceof ErroNaoAutenticado) aoExpirar()
        // Link velho (?id= de uma candidatura excluída): fecha em vez de abrir um painel vazio.
        else if (e.status === 404) aoNaoEncontrada()
        else setErro(e.message)
      })
    return () => {
      cancelado = true
    }
  }, [id, versao, aoExpirar, aoNaoEncontrada])

  /** Roda uma alteração; devolve true se deu certo. */
  async function executar(acao) {
    setOcupado(true)
    setErro(null)
    try {
      await acao()
      return true
    } catch (e) {
      if (e instanceof ErroNaoAutenticado) aoExpirar()
      else setErro(e.message)
      return false
    } finally {
      setOcupado(false)
    }
  }

  function aplicar(dados) {
    setDetalhe(dados)
    aoAtualizar(dados.candidatura)
  }

  const candidatura = detalhe?.candidatura

  return (
    <dialog
      {...propsDialogo}
      className="painel"
      aria-label={candidatura ? `${candidatura.titulo} em ${candidatura.empresa}` : 'Detalhes da candidatura'}
    >
      <div className="painel-caixa">
        <header className="painel-cabecalho">
          <div className="painel-cabecalho-linha">
            {candidatura && (
              <span className="chip-etapa" data-etapa={candidatura.etapa}>
                {ROTULO_ETAPA[candidatura.etapa]}
              </span>
            )}
            <button type="button" className="dialogo-fechar" onClick={aoFechar} aria-label="Fechar detalhes">
              <IconeLimpar tamanho={14} />
            </button>
          </div>
          {candidatura ? (
            <>
              <p className="painel-empresa">{candidatura.empresa}</p>
              <h2 className="painel-titulo">{candidatura.titulo}</h2>
              <p className="painel-meta">
                {candidatura.local && <span>{candidatura.local}</span>}
                <span>{candidatura.externa ? 'vaga externa' : candidatura.fonte?.toLowerCase()}</span>
                {candidatura.link && (
                  <a href={candidatura.link} target="_blank" rel="noreferrer">
                    abrir vaga <IconeSetaExterna tamanho={11} />
                  </a>
                )}
              </p>
              <a className="botao painel-curriculo" href={`/curriculo?aba=adaptar&candidatura=${candidatura.id}`}>
                adaptar meu currículo para esta vaga
              </a>
              {vagaSaiuDoAr(candidatura) && (
                <p className="mensagem">A vaga saiu do ar na fonte. A candidatura continua aqui.</p>
              )}
            </>
          ) : (
            !erro && <p className="painel-fraco">carregando...</p>
          )}
        </header>

        {erro && (
          <p className="mensagem mensagem-erro" role="alert">
            {erro}
          </p>
        )}

        {candidatura && (
          <div className="painel-corpo" key={`${candidatura.id}-${candidatura.atualizadaEm}`}>
            <Etapas
              candidatura={candidatura}
              ocupado={ocupado}
              aoMudar={(etapa) => executar(async () => aplicar(await atualizarCandidatura(id, { etapa })))}
            />

            <section className="painel-secao">
              <h3 className="painel-secao-titulo">candidatura enviada em</h3>
              <div className="painel-data-linha">
                <label className="campo painel-campo-data">
                  <span className="sr-only">Data da candidatura</span>
                  <input
                    type="date"
                    max={hojeIso()}
                    value={candidatura.dataCandidatura ?? ''}
                    disabled={ocupado}
                    onChange={(e) => {
                      const valor = e.target.value
                      // Apagar pelo próprio campo também limpa (o input date fica vazio).
                      const corpo = valor ? { dataCandidatura: valor } : { limparDataCandidatura: true }
                      executar(async () => aplicar(await atualizarCandidatura(id, corpo)))
                    }}
                  />
                </label>
                {candidatura.dataCandidatura && (
                  <button
                    type="button"
                    className="botao-limpar"
                    disabled={ocupado}
                    onClick={() =>
                      executar(async () => aplicar(await atualizarCandidatura(id, { limparDataCandidatura: true })))
                    }
                  >
                    não enviei
                  </button>
                )}
              </div>
              {!candidatura.dataCandidatura && (
                <p className="painel-fraco">Preenchida sozinha quando você move para "candidatei".</p>
              )}
            </section>

            <ProximoPasso
              candidatura={candidatura}
              ocupado={ocupado}
              aoSalvar={(texto, data) => executar(async () => aplicar(await definirProximoPasso(id, texto, data)))}
            />

            {candidatura.externa && (
              <DadosExterna
                candidatura={candidatura}
                ocupado={ocupado}
                aoSalvar={(dados) => executar(async () => aplicar(await atualizarCandidatura(id, dados)))}
              />
            )}

            <LinhaDoTempo
              eventos={detalhe.eventos}
              ocupado={ocupado}
              aoAdicionar={(texto) =>
                executar(async () => {
                  const nota = await adicionarNota(id, texto)
                  setDetalhe((atual) => ({ ...atual, eventos: [...atual.eventos, nota] }))
                })
              }
              aoApagar={(notaId) =>
                executar(async () => {
                  await apagarNota(id, notaId)
                  setDetalhe((atual) => ({ ...atual, eventos: atual.eventos.filter((e) => e.id !== notaId) }))
                })
              }
            />

            <footer className="painel-rodape">
              <span className="painel-fraco">adicionada {formatarDias(diasDesde(candidatura.criadaEm))}</span>
              <button
                type="button"
                className="botao botao-perigo"
                disabled={ocupado}
                onClick={() => {
                  if (!window.confirm('Excluir esta candidatura e toda a linha do tempo dela?')) return
                  executar(async () => {
                    await excluirCandidatura(id)
                    aoExcluir(id)
                  })
                }}
              >
                <IconeLixeira tamanho={12} /> excluir
              </button>
            </footer>
          </div>
        )}
      </div>
    </dialog>
  )
}
