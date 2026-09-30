import { useEffect, useState } from 'react'
import { ErroNaoAutenticado, chamarAdmin } from './api'
import {
  ORIGEM_LABEL,
  SAUDE_LABEL,
  formatarDataHora,
  formatarDuracao,
  formatarHora,
  formatarRelativo,
} from './formato'

// Polling mais rápido só enquanto há coleta rodando, para acompanhar o progresso.
const INTERVALO_RODANDO_MS = 3000
const INTERVALO_PARADO_MS = 30000
const DURACAO_AVISO_MS = 6000

function CardCrawler({ crawler, agendamentoPausado, bloqueado, acao, aoColetar }) {
  const ultima = crawler.ultimaExecucao
  const chaveAcao = `coletar-${crawler.fonte}`

  return (
    <li className="crawler" data-saude={crawler.saude}>
      <div className="crawler-cabecalho">
        <h2 className="crawler-fonte">{crawler.fonte}</h2>
        <span className="crawler-saude" data-saude={crawler.saude}>
          {SAUDE_LABEL[crawler.saude] ?? crawler.saude}
        </span>
      </div>

      {crawler.rodando ? (
        <p className="crawler-rodando">
          <span className="pulso" aria-hidden="true" />
          coletando{crawler.progresso ? ` · ${crawler.progresso}` : '...'}
        </p>
      ) : (
        <p className="crawler-estado">{crawler.ligada ? 'ligada' : 'desligada'}</p>
      )}

      <dl className="crawler-dados">
        <dt>última execução</dt>
        <dd>
          {ultima ? (
            <>
              {formatarDataHora(ultima.inicio)}{' '}
              <span className="admin-fraco">({formatarRelativo(ultima.inicio)})</span>
            </>
          ) : (
            'nunca rodou'
          )}
        </dd>

        {ultima && (
          <>
            <dt>duração</dt>
            <dd>{ultima.fim ? formatarDuracao(ultima.inicio, ultima.fim) : 'em andamento'}</dd>

            <dt>resultado</dt>
            <dd>
              {ultima.encontradas} encontradas · {ultima.novas} novas · {ultima.expiradas} expiradas
            </dd>
          </>
        )}

        <dt>último sucesso</dt>
        <dd>{crawler.ultimoSucesso ? formatarDataHora(crawler.ultimoSucesso) : '—'}</dd>

        <dt>próxima coleta</dt>
        <dd>
          {!crawler.ligada
            ? '—'
            : agendamentoPausado
              ? 'agendamento pausado'
              : crawler.proximaColeta
                ? `${formatarHora(crawler.proximaColeta)} (${formatarRelativo(crawler.proximaColeta)})`
                : '—'}
        </dd>
      </dl>

      {ultima?.mensagemErro && <p className="crawler-erro">{ultima.mensagemErro}</p>}

      <button
        type="button"
        className="admin-botao crawler-botao"
        onClick={() => aoColetar(crawler.fonte)}
        disabled={!crawler.ligada || bloqueado}
        title={crawler.ligada ? undefined : 'Fonte desligada'}
      >
        {acao === chaveAcao ? 'disparando...' : 'coletar agora'}
      </button>
    </li>
  )
}

export default function Painel({ aoExpirar }) {
  const [painel, setPainel] = useState(null)
  const [erro, setErro] = useState(null)
  const [acao, setAcao] = useState(null)
  const [aviso, setAviso] = useState(null)
  // Incrementar força uma consulta imediata (depois de uma ação) e reinicia o polling.
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let cancelado = false
    let temporizador

    async function carregar() {
      let rodando = false
      try {
        const dados = await chamarAdmin('/crawlers')
        if (cancelado) return
        setPainel(dados)
        setErro(null)
        rodando = dados.coletaEmAndamento != null
      } catch (e) {
        if (cancelado) return
        if (e instanceof ErroNaoAutenticado) {
          aoExpirar()
          return
        }
        setErro(e.message)
      }
      temporizador = setTimeout(carregar, rodando ? INTERVALO_RODANDO_MS : INTERVALO_PARADO_MS)
    }

    carregar()
    return () => {
      cancelado = true
      clearTimeout(temporizador)
    }
  }, [versao, aoExpirar])

  // Confirmações somem sozinhas; erros ficam até a próxima ação.
  useEffect(() => {
    if (aviso?.tipo !== 'ok') return
    const temporizador = setTimeout(() => setAviso(null), DURACAO_AVISO_MS)
    return () => clearTimeout(temporizador)
  }, [aviso])

  async function executar(chave, caminho, textoSucesso) {
    setAcao(chave)
    setAviso(null)
    try {
      const resposta = await chamarAdmin(caminho, { method: 'POST' })
      setAviso({ tipo: 'ok', texto: resposta?.mensagem ?? textoSucesso })
    } catch (e) {
      if (e instanceof ErroNaoAutenticado) {
        aoExpirar()
        return
      }
      setAviso({ tipo: 'erro', texto: e.message })
    } finally {
      setAcao(null)
      setVersao((v) => v + 1)
    }
  }

  if (!painel) {
    return erro ? (
      <div className="nota nota-alerta">
        <p className="nota-titulo">falha ao carregar o painel</p>
        <p className="nota-texto">{erro}</p>
      </div>
    ) : (
      <p className="admin-carregando">carregando painel...</p>
    )
  }

  const coleta = painel.coletaEmAndamento
  const bloqueado = coleta != null || acao != null

  return (
    <section aria-label="Painel dos crawlers">
      <div className="admin-barra">
        <p className="status-linha admin-status" aria-live="polite">
          <span className="pulso" data-status={erro ? 'alerta' : coleta ? 'sincronizando' : 'ativo'} aria-hidden="true" />
          {erro
            ? `conexão instável: ${erro}`
            : coleta
              ? `coleta ${ORIGEM_LABEL[coleta.origem] ?? coleta.origem} desde ${formatarHora(coleta.inicio)}` +
                (coleta.fonteAtual ? ` · ${coleta.fonteAtual}` : '') +
                (coleta.progresso ? ` · ${coleta.progresso}` : '')
              : painel.agendamentoPausado
                ? 'parado · agendamento pausado'
                : 'parado · aguardando a próxima coleta'}
        </p>

        <div className="admin-acoes">
          <button
            type="button"
            className="admin-botao admin-botao-primario"
            disabled={bloqueado}
            onClick={() => executar('coletar-todas', '/coletas', 'Coleta iniciada')}
          >
            {acao === 'coletar-todas' ? 'disparando...' : 'coletar todas agora'}
          </button>
          {painel.agendamentoPausado ? (
            <button
              type="button"
              className="admin-botao"
              disabled={acao != null}
              onClick={() => executar('agendamento', '/agendamento/retomar', 'Agendamento retomado')}
            >
              retomar agendamento
            </button>
          ) : (
            <button
              type="button"
              className="admin-botao"
              disabled={acao != null}
              onClick={() => executar('agendamento', '/agendamento/pausar', 'Agendamento pausado')}
            >
              pausar agendamento
            </button>
          )}
        </div>
      </div>

      {aviso && (
        <p className={`admin-mensagem admin-mensagem-${aviso.tipo}`} role="status">
          {aviso.texto}
        </p>
      )}

      <ul className="crawlers">
        {painel.crawlers.map((crawler) => (
          <CardCrawler
            key={crawler.fonte}
            crawler={crawler}
            agendamentoPausado={painel.agendamentoPausado}
            bloqueado={bloqueado}
            acao={acao}
            aoColetar={(fonte) =>
              executar(`coletar-${fonte}`, `/coletas/${encodeURIComponent(fonte)}`, 'Coleta iniciada')
            }
          />
        ))}
      </ul>
    </section>
  )
}
