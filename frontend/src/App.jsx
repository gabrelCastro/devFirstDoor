import { useEffect, useMemo, useRef, useState } from 'react'
import './App.css'
import { NIVEL_LABEL, ehRecente, formatarData, formatarIndice } from './utils'
import {
  IconeAlerta,
  IconeBusca,
  IconeCaixaVazia,
  IconeCalendario,
  IconeLimpar,
  IconeLua,
  IconePin,
  IconeSetaExterna,
  IconeSol,
} from './icons'

function useTema() {
  const [tema, setTema] = useState(() => {
    try {
      const salvo = localStorage.getItem('tema')
      if (salvo === 'claro' || salvo === 'escuro') return salvo
    } catch {
      // ignora falha ao ler localStorage (modo privado, etc.)
    }
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'escuro' : 'claro'
  })

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', tema)
    try {
      localStorage.setItem('tema', tema)
    } catch {
      // ignora falha ao gravar no localStorage
    }
  }, [tema])

  return [tema, () => setTema((atual) => (atual === 'claro' ? 'escuro' : 'claro'))]
}

const TAMANHO_PAGINA = 30
const ATRASO_BUSCA_MS = 300

function urlDaApi(caminho, parametros) {
  const busca = new URLSearchParams()
  for (const [chave, valor] of Object.entries(parametros)) {
    if (valor !== '' && valor != null) busca.set(chave, valor)
  }
  return `${caminho}?${busca}`
}

async function buscarJson(url) {
  const resposta = await fetch(url)
  if (!resposta.ok) {
    throw new Error(`API respondeu com status ${resposta.status}`)
  }
  return resposta.json()
}

const CHARSET_DECODIFICACAO ='ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#/'

function useDecodificarTitulo(textoFinal, atrasoMs = 1550, duracaoMs = 900) {
  const ref = useRef(null)

  useEffect(() => {
    const el = ref.current
    if (!el) return

    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
      el.textContent = textoFinal
      return
    }

    let quadro
    let cancelado = false

    function passo(agora, inicio) {
      if (cancelado) return
      const progresso = Math.min(1, (agora - inicio) / duracaoMs)
      const travadas = Math.floor(progresso * textoFinal.length)
      let saida = ''
      for (let i = 0; i < textoFinal.length; i++) {
        const caractere = textoFinal[i]
        saida +=
          i < travadas || caractere === ' '
            ? caractere
            : CHARSET_DECODIFICACAO[Math.floor(Math.random() * CHARSET_DECODIFICACAO.length)]
      }
      el.textContent = saida
      if (progresso < 1) {
        quadro = requestAnimationFrame((t) => passo(t, inicio))
      } else {
        el.textContent = textoFinal
      }
    }

    const temporizador = setTimeout(() => {
      quadro = requestAnimationFrame((t) => passo(t, t))
    }, atrasoMs)

    return () => {
      cancelado = true
      clearTimeout(temporizador)
      if (quadro) cancelAnimationFrame(quadro)
    }
  }, [textoFinal, atrasoMs, duracaoMs])

  return ref
}

function LinhaSkeleton({ indice }) {
  return (
    <li className="entrada entrada-skeleton" aria-hidden="true">
      <span className="entrada-num">{formatarIndice(indice)}</span>
      <div className="entrada-corpo">
        <div className="traco traco-selo" />
        <div className="traco traco-titulo" />
        <div className="traco traco-empresa" />
      </div>
    </li>
  )
}

export default function App() {
  const [tema, alternarTema] = useTema()
  const tituloRef = useDecodificarTitulo('Dev First Door')
  const [vagas, setVagas] = useState([])
  const [pagina, setPagina] = useState(null)
  const [contagens, setContagens] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [atualizando, setAtualizando] = useState(false)
  const [carregandoMais, setCarregandoMais] = useState(false)
  const [erro, setErro] = useState(null)
  const [busca, setBusca] = useState('')
  const [buscaAplicada, setBuscaAplicada] = useState('')
  const [secaoFiltro, setSecaoFiltro] = useState('TODAS')
  const [escopoFiltro, setEscopoFiltro] = useState('TODAS')

  // Só consulta a API quando o usuário para de digitar.
  useEffect(() => {
    const temporizador = setTimeout(() => setBuscaAplicada(busca.trim()), ATRASO_BUSCA_MS)
    return () => clearTimeout(temporizador)
  }, [busca])

  const filtros = useMemo(
    () => ({ secao: secaoFiltro, escopo: escopoFiltro, q: buscaAplicada }),
    [secaoFiltro, escopoFiltro, buscaAplicada],
  )

  useEffect(() => {
    let cancelado = false

    async function carregarVagas() {
      setAtualizando(true)
      try {
        const [primeiraPagina, novasContagens] = await Promise.all([
          buscarJson(urlDaApi('/api/vagas', { ...filtros, page: 0, size: TAMANHO_PAGINA })),
          buscarJson(urlDaApi('/api/vagas/contagens', filtros)),
        ])
        if (!cancelado) {
          setVagas(primeiraPagina.content ?? [])
          setPagina(primeiraPagina)
          setContagens(novasContagens)
          setErro(null)
        }
      } catch (e) {
        if (!cancelado) {
          setErro(e.message)
        }
      } finally {
        if (!cancelado) {
          setCarregando(false)
          setAtualizando(false)
        }
      }
    }

    carregarVagas()
    return () => {
      cancelado = true
    }
  }, [filtros])

  const filtrosRef = useRef(filtros)
  useEffect(() => {
    filtrosRef.current = filtros
  }, [filtros])

  async function carregarMais() {
    if (!pagina || carregandoMais) return
    const filtrosDaPagina = filtros
    setCarregandoMais(true)
    try {
      const proxima = await buscarJson(
        urlDaApi('/api/vagas', { ...filtrosDaPagina, page: pagina.number + 1, size: TAMANHO_PAGINA }),
      )
      // Descarta a resposta se os filtros mudaram enquanto ela chegava.
      if (filtrosRef.current !== filtrosDaPagina) return
      setVagas((atuais) => [...atuais, ...(proxima.content ?? [])])
      setPagina(proxima)
    } catch (e) {
      setErro(e.message)
    } finally {
      setCarregandoMais(false)
    }
  }

  const totalGeral = contagens?.total ?? 0
  const totalFiltrado = pagina?.totalElements ?? 0
  const fontes = contagens?.fontes ?? []
  const haMais = pagina != null && !pagina.last

  const filtrosAtivos = busca.trim() !== '' || secaoFiltro !== 'TODAS' || escopoFiltro !== 'TODAS'

  function limparFiltros() {
    setBusca('')
    setSecaoFiltro('TODAS')
    setEscopoFiltro('TODAS')
  }

  const statusConexao = carregando ? 'sincronizando' : erro ? 'alerta' : 'ativo'
  const statusTexto = carregando
    ? 'sincronizando fontes...'
    : erro
      ? 'conexão instável com o servidor'
      : `${fontes.length} ${fontes.length === 1 ? 'fonte conectada' : 'fontes conectadas'} · ${totalGeral} ${totalGeral === 1 ? 'vaga no radar' : 'vagas no radar'}`

  return (
    <div className="pagina">
      <header className="cabecalho">
        <button
          type="button"
          className="botao-tema"
          onClick={alternarTema}
          aria-label={tema === 'claro' ? 'Ativar tema escuro' : 'Ativar tema claro'}
          title={tema === 'claro' ? 'Ativar tema escuro' : 'Ativar tema claro'}
        >
          {tema === 'claro' ? <IconeLua tamanho={15} /> : <IconeSol tamanho={15} />}
        </button>

        <div className="boot" aria-hidden="true">
          <p className="boot-linha">$ conectando gupy · remoteok · programathor</p>
          <p className="boot-linha boot-ok">→ 3 fontes online</p>
          <p className="boot-linha">$ carregando manifesto...</p>
        </div>
        <h1 className="boot-titulo" ref={tituloRef}>
          Dev First Door
        </h1>

        <p className="status-linha" aria-live="polite">
          <span className="pulso" data-status={statusConexao} aria-hidden="true" />
          {statusTexto}
        </p>
      </header>

      <main>
        {!carregando && !erro && totalGeral > 0 && (
          <div className="console">
            <label className="console-busca">
              <span className="sr-only">Buscar vagas</span>
              <span className="prompt" aria-hidden="true">
                ❯
              </span>
              <input
                type="search"
                placeholder="buscar cargo, empresa ou local..."
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
              />
            </label>

            <div className="abas-grupo" role="group" aria-label="Filtrar por seção">
              <button
                type="button"
                className={`aba ${secaoFiltro === 'TODAS' ? 'aba-ativa' : ''}`}
                onClick={() => setSecaoFiltro('TODAS')}
              >
                todas <span className="aba-contagem">{contagens.secao.TODAS}</span>
              </button>
              <button
                type="button"
                className={`aba ${secaoFiltro === 'REMOTO' ? 'aba-ativa' : ''}`}
                onClick={() => setSecaoFiltro('REMOTO')}
              >
                remoto <span className="aba-contagem">{contagens.secao.REMOTO}</span>
              </button>
              <button
                type="button"
                className={`aba ${secaoFiltro === 'ESTAGIO' ? 'aba-ativa' : ''}`}
                onClick={() => setSecaoFiltro('ESTAGIO')}
              >
                estágio <span className="aba-contagem">{contagens.secao.ESTAGIO}</span>
              </button>
            </div>

            <div className="abas-grupo" role="group" aria-label="Filtrar por abrangência">
              <button
                type="button"
                className={`aba ${escopoFiltro === 'TODAS' ? 'aba-ativa' : ''}`}
                onClick={() => setEscopoFiltro('TODAS')}
              >
                todas
              </button>
              <button
                type="button"
                className={`aba ${escopoFiltro === 'NACIONAL' ? 'aba-ativa' : ''}`}
                onClick={() => setEscopoFiltro('NACIONAL')}
              >
                nacional <span className="aba-contagem">{contagens.escopo.NACIONAL}</span>
              </button>
              <button
                type="button"
                className={`aba ${escopoFiltro === 'GRINGA' ? 'aba-ativa' : ''}`}
                onClick={() => setEscopoFiltro('GRINGA')}
              >
                gringa <span className="aba-contagem">{contagens.escopo.GRINGA}</span>
              </button>
            </div>

            {filtrosAtivos && (
              <button type="button" className="botao-limpar" onClick={limparFiltros}>
                <IconeLimpar tamanho={12} />
                limpar
              </button>
            )}
          </div>
        )}

        {!carregando && !erro && totalGeral > 0 && filtrosAtivos && (
          <p className="contagem-resultados" aria-live="polite">
            {totalFiltrado} de {totalGeral} entradas
          </p>
        )}

        {carregando && (
          <ol className="manifesto" aria-busy="true" aria-label="Carregando manifesto de vagas">
            {Array.from({ length: 6 }, (_, i) => (
              <LinhaSkeleton key={i} indice={i + 1} />
            ))}
          </ol>
        )}

        {!carregando && erro && (
          <div className="nota nota-alerta">
            <IconeAlerta tamanho={28} />
            <p className="nota-titulo">falha ao carregar o manifesto</p>
            <p className="nota-texto">{erro}. O backend já rodou a coleta inicial?</p>
          </div>
        )}

        {!carregando && !erro && totalGeral === 0 && (
          <div className="nota">
            <IconeCaixaVazia tamanho={28} />
            <p className="nota-titulo">nenhuma vaga coletada ainda</p>
            <p className="nota-texto">Assim que a coleta rodar, as vagas aparecem aqui.</p>
          </div>
        )}

        {!carregando && !erro && totalGeral > 0 && vagas.length === 0 && (
          <div className="nota">
            <IconeBusca tamanho={28} />
            <p className="nota-titulo">nenhuma entrada encontrada</p>
            <p className="nota-texto">Tente ajustar a busca, a seção ou a abrangência.</p>
          </div>
        )}

        {!carregando && !erro && vagas.length > 0 && (
          <ol className="manifesto" aria-busy={atualizando}>
            {vagas.map((vaga, indice) => (
              <li key={vaga.id} className="entrada" style={{ '--atraso': `${Math.min(indice, 10) * 35}ms` }}>
                <span className="entrada-num">{formatarIndice(indice + 1)}</span>
                <div className="entrada-corpo">
                  <div className="entrada-cabecalho">
                    <span className={`selo selo-${vaga.nivel.toLowerCase()}`}>
                      {NIVEL_LABEL[vaga.nivel] ?? vaga.nivel}
                    </span>
                    {vaga.internacional && <span className="selo selo-gringa">Gringa</span>}
                    {ehRecente(vaga.dataColeta) && <span className="carimbo-novo">novo</span>}
                    <span className="entrada-fonte">{vaga.fonte}</span>
                  </div>

                  <h2 className="entrada-titulo">{vaga.titulo}</h2>

                  <p className="entrada-detalhe">
                    <span className="entrada-empresa">{vaga.empresa}</span>
                    {vaga.local && (
                      <span className="entrada-local">
                        <IconePin tamanho={12} />
                        {vaga.local}
                      </span>
                    )}
                  </p>

                  <div className="entrada-rodape">
                    <time className="entrada-data" dateTime={vaga.dataPublicacao ?? undefined}>
                      <IconeCalendario tamanho={12} />
                      {formatarData(vaga.dataPublicacao)}
                    </time>
                    <a className="entrada-link" href={vaga.link} target="_blank" rel="noreferrer">
                      ver vaga
                      <IconeSetaExterna tamanho={12} />
                    </a>
                  </div>
                </div>
              </li>
            ))}
          </ol>
        )}

        {!carregando && !erro && haMais && (
          <div className="carregar-mais">
            <button
              type="button"
              className="botao-carregar-mais"
              onClick={carregarMais}
              disabled={carregandoMais || atualizando}
            >
              {carregandoMais ? 'carregando...' : `carregar mais (${vagas.length} de ${totalFiltrado})`}
            </button>
          </div>
        )}
      </main>

      <footer className="rodape">
        <p>
          {fontes.length > 0 ? fontes.join(' · ') : 'gupy · remoteok · programathor'}
        </p>
        <p className="rodape-fim">— fim do manifesto —</p>
      </footer>
    </div>
  )
}
