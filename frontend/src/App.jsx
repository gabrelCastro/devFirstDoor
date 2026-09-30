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

const CHARSET_DECODIFICACAO = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#/'

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
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)
  const [busca, setBusca] = useState('')
  const [secaoFiltro, setSecaoFiltro] = useState('TODAS')
  const [escopoFiltro, setEscopoFiltro] = useState('TODAS')

  useEffect(() => {
    let cancelado = false

    async function carregarVagas() {
      try {
        const resposta = await fetch('/api/vagas?size=50')
        if (!resposta.ok) {
          throw new Error(`API respondeu com status ${resposta.status}`)
        }
        const pagina = await resposta.json()
        if (!cancelado) {
          setVagas(pagina.content ?? [])
        }
      } catch (e) {
        if (!cancelado) {
          setErro(e.message)
        }
      } finally {
        if (!cancelado) {
          setCarregando(false)
        }
      }
    }

    carregarVagas()
    return () => {
      cancelado = true
    }
  }, [])

  const contagemPorSecao = useMemo(() => {
    return vagas.reduce(
      (acc, vaga) => {
        if (vaga.remoto) acc.REMOTO += 1
        if (vaga.nivel === 'ESTAGIO') acc.ESTAGIO += 1
        return acc
      },
      { REMOTO: 0, ESTAGIO: 0 },
    )
  }, [vagas])

  const contagemPorEscopo = useMemo(() => {
    return vagas.reduce(
      (acc, vaga) => {
        if (vaga.internacional) acc.GRINGA += 1
        else acc.NACIONAL += 1
        return acc
      },
      { NACIONAL: 0, GRINGA: 0 },
    )
  }, [vagas])

  const vagasFiltradas = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    return vagas.filter((vaga) => {
      if (secaoFiltro === 'REMOTO' && !vaga.remoto) return false
      if (secaoFiltro === 'ESTAGIO' && vaga.nivel !== 'ESTAGIO') return false
      if (escopoFiltro === 'NACIONAL' && vaga.internacional) return false
      if (escopoFiltro === 'GRINGA' && !vaga.internacional) return false
      if (termo) {
        const alvo = `${vaga.titulo} ${vaga.empresa} ${vaga.local ?? ''}`.toLowerCase()
        if (!alvo.includes(termo)) return false
      }
      return true
    })
  }, [vagas, busca, secaoFiltro, escopoFiltro])

  const fontesUnicas = useMemo(
    () => Array.from(new Set(vagas.map((v) => v.fonte))).sort(),
    [vagas],
  )

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
      : `${fontesUnicas.length} ${fontesUnicas.length === 1 ? 'fonte conectada' : 'fontes conectadas'} · ${vagas.length} ${vagas.length === 1 ? 'vaga no radar' : 'vagas no radar'}`

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
        {!carregando && !erro && vagas.length > 0 && (
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
                todas <span className="aba-contagem">{vagas.length}</span>
              </button>
              <button
                type="button"
                className={`aba ${secaoFiltro === 'REMOTO' ? 'aba-ativa' : ''}`}
                onClick={() => setSecaoFiltro('REMOTO')}
              >
                remoto <span className="aba-contagem">{contagemPorSecao.REMOTO}</span>
              </button>
              <button
                type="button"
                className={`aba ${secaoFiltro === 'ESTAGIO' ? 'aba-ativa' : ''}`}
                onClick={() => setSecaoFiltro('ESTAGIO')}
              >
                estágio <span className="aba-contagem">{contagemPorSecao.ESTAGIO}</span>
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
                nacional <span className="aba-contagem">{contagemPorEscopo.NACIONAL}</span>
              </button>
              <button
                type="button"
                className={`aba ${escopoFiltro === 'GRINGA' ? 'aba-ativa' : ''}`}
                onClick={() => setEscopoFiltro('GRINGA')}
              >
                gringa <span className="aba-contagem">{contagemPorEscopo.GRINGA}</span>
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

        {!carregando && !erro && vagas.length > 0 && filtrosAtivos && (
          <p className="contagem-resultados" aria-live="polite">
            {vagasFiltradas.length} de {vagas.length} entradas
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

        {!carregando && !erro && vagas.length === 0 && (
          <div className="nota">
            <IconeCaixaVazia tamanho={28} />
            <p className="nota-titulo">nenhuma vaga coletada ainda</p>
            <p className="nota-texto">Assim que a coleta rodar, as vagas aparecem aqui.</p>
          </div>
        )}

        {!carregando && !erro && vagas.length > 0 && vagasFiltradas.length === 0 && (
          <div className="nota">
            <IconeBusca tamanho={28} />
            <p className="nota-titulo">nenhuma entrada encontrada</p>
            <p className="nota-texto">Tente ajustar a busca, a seção ou a abrangência.</p>
          </div>
        )}

        {!carregando && !erro && vagasFiltradas.length > 0 && (
          <ol className="manifesto">
            {vagasFiltradas.map((vaga, indice) => (
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
      </main>

      <footer className="rodape">
        <p>
          {fontesUnicas.length > 0 ? fontesUnicas.join(' · ') : 'gupy · remoteok · programathor'}
        </p>
        <p className="rodape-fim">— fim do manifesto —</p>
      </footer>
    </div>
  )
}
