import { useCallback, useEffect, useState } from 'react'
import '../App.css'
import '../conta/conta.css'
import './etapas.css'
import './Candidaturas.css'
import { useTema } from '../tema'
import {
  IconeAlerta,
  IconeBusca,
  IconeDownload,
  IconeLimpar,
  IconeLista,
  IconeLua,
  IconeMais,
  IconeMarcador,
  IconeQuadro,
  IconeSol,
} from '../icons'
import MenuConta from '../conta/MenuConta'
import { useSessao } from '../conta/useSessao'
import { ErroNaoAutenticado } from '../conta/sessao'
import { atualizarCandidatura, baixarCsv, buscarResumo, listarCandidaturas } from './api'
import { ROTULO_ETAPA, filtrarCandidaturas, moverLocalmente, substituir } from './candidaturasDados'
import Resumo from './Resumo'
import Quadro from './Quadro'
import ListaCandidaturas from './ListaCandidaturas'
import PainelCandidatura from './PainelCandidatura'
import ModalExterna from './ModalExterna'

const CHAVE_VISAO = 'candidaturas-visao'
const CONSULTA_CELULAR = '(max-width: 760px)'

function lerVisao() {
  try {
    return localStorage.getItem(CHAVE_VISAO) === 'lista' ? 'lista' : 'quadro'
  } catch {
    return 'quadro'
  }
}

function useCelular() {
  const [celular, setCelular] = useState(() => window.matchMedia?.(CONSULTA_CELULAR).matches ?? false)
  useEffect(() => {
    const consulta = window.matchMedia?.(CONSULTA_CELULAR)
    if (!consulta) return
    const aoMudar = () => setCelular(consulta.matches)
    consulta.addEventListener('change', aoMudar)
    return () => consulta.removeEventListener('change', aoMudar)
  }, [])
  return celular
}

/** ?id= abre o painel direto (links da página de vagas e do aviso de vaga salva). */
function idDaUrl() {
  const id = Number(new URLSearchParams(window.location.search).get('id'))
  return Number.isInteger(id) && id > 0 ? id : null
}

function atualizarUrl(id) {
  const url = new URL(window.location.href)
  if (id) url.searchParams.set('id', id)
  else url.searchParams.delete('id')
  window.history.replaceState(null, '', url)
}

function Cabecalho() {
  const [tema, alternarTema] = useTema()
  return (
    <header className="topo">
      <div className="topo-marca">
        <a href="/" className="topo-voltar">
          Dev First Door
        </a>
        <span className="topo-sep" aria-hidden="true">
          /
        </span>
        <h1 className="topo-titulo">candidaturas</h1>
      </div>
      <div className="cabecalho-acoes-linha">
        <MenuConta paginaAtual="candidaturas" />
        <button
          type="button"
          className="botao-tema"
          onClick={alternarTema}
          aria-label={tema === 'claro' ? 'Ativar tema escuro' : 'Ativar tema claro'}
          title={tema === 'claro' ? 'Ativar tema escuro' : 'Ativar tema claro'}
        >
          {tema === 'claro' ? <IconeLua tamanho={15} /> : <IconeSol tamanho={15} />}
        </button>
      </div>
    </header>
  )
}

function Convite() {
  const { pedirLogin } = useSessao()
  return (
    <div className="convite">
      <IconeMarcador tamanho={30} />
      <p className="nota-titulo">seu quadro de candidaturas</p>
      <p className="nota-texto">
        Salve vagas, mova cada uma pelas etapas do processo, anote o que aconteceu e veja quando é hora de cobrar um
        retorno.
      </p>
      <div className="convite-acoes">
        <button type="button" className="botao botao-primario" onClick={() => pedirLogin({ aba: 'criar' })}>
          criar conta
        </button>
        <button type="button" className="botao" onClick={() => pedirLogin()}>
          já tenho conta
        </button>
      </div>
    </div>
  )
}

function Quadros() {
  const { sair } = useSessao()
  const celular = useCelular()
  const [candidaturas, setCandidaturas] = useState(null)
  const [resumo, setResumo] = useState(null)
  const [erro, setErro] = useState(null)
  const [aviso, setAviso] = useState(null)
  const [busca, setBusca] = useState('')
  const [visaoEscolhida, setVisaoEscolhida] = useState(lerVisao)
  const [abertaId, setAbertaId] = useState(idDaUrl)
  const [versaoPainel, setVersaoPainel] = useState(0)
  const [criandoExterna, setCriandoExterna] = useState(false)
  const [movendo, setMovendo] = useState(null)
  const [exportando, setExportando] = useState(false)
  const visao = celular ? 'lista' : visaoEscolhida

  const expirar = useCallback(() => {
    sair()
  }, [sair])

  const tratarErro = useCallback(
    (e) => {
      if (e instanceof ErroNaoAutenticado) expirar()
      else setAviso({ texto: e.message, erro: true })
    },
    [expirar],
  )

  const recarregarResumo = useCallback(() => {
    buscarResumo().then(setResumo).catch(() => {})
  }, [])

  useEffect(() => {
    let cancelado = false
    Promise.all([listarCandidaturas(), buscarResumo()])
      .then(([lista, dadosResumo]) => {
        if (cancelado) return
        setCandidaturas(lista)
        setResumo(dadosResumo)
      })
      .catch((e) => {
        if (cancelado) return
        if (e instanceof ErroNaoAutenticado) expirar()
        else setErro(e.message)
      })
    return () => {
      cancelado = true
    }
  }, [expirar])

  useEffect(() => {
    if (!aviso || aviso.erro) return
    const temporizador = setTimeout(() => setAviso(null), 5000)
    return () => clearTimeout(temporizador)
  }, [aviso])

  function escolherVisao(nova) {
    setVisaoEscolhida(nova)
    try {
      localStorage.setItem(CHAVE_VISAO, nova)
    } catch {
      // a escolha vale só enquanto a página estiver aberta
    }
  }

  function abrir(id) {
    setAbertaId(id)
    atualizarUrl(id)
  }

  function fechar() {
    setAbertaId(null)
    atualizarUrl(null)
  }

  /** Atualização otimista: o cartão muda de coluna na hora e volta se o servidor recusar. */
  async function mover(candidatura, etapa) {
    const anterior = candidaturas
    setCandidaturas((atuais) => moverLocalmente(atuais, candidatura.id, etapa))
    setMovendo(candidatura.id)
    try {
      const { candidatura: atualizada } = await atualizarCandidatura(candidatura.id, { etapa })
      setCandidaturas((atuais) => substituir(atuais, atualizada))
      setAviso({ texto: `${candidatura.empresa} → ${ROTULO_ETAPA[etapa]}` })
      if (abertaId === candidatura.id) setVersaoPainel((v) => v + 1)
      recarregarResumo()
    } catch (e) {
      setCandidaturas(anterior)
      tratarErro(e)
    } finally {
      setMovendo(null)
    }
  }

  function aoAtualizar(atualizada) {
    setCandidaturas((atuais) => substituir(atuais, atualizada))
    recarregarResumo()
  }

  function aoExcluir(id) {
    setCandidaturas((atuais) => atuais.filter((c) => c.id !== id))
    fechar()
    setAviso({ texto: 'Candidatura excluída.' })
    recarregarResumo()
  }

  function aoCriar(nova) {
    setCriandoExterna(false)
    setCandidaturas((atuais) => [nova, ...atuais])
    recarregarResumo()
    abrir(nova.id)
  }

  async function exportar() {
    setExportando(true)
    try {
      await baixarCsv()
    } catch (e) {
      tratarErro(e)
    } finally {
      setExportando(false)
    }
  }

  if (erro) {
    return (
      <div className="nota nota-alerta">
        <IconeAlerta tamanho={28} />
        <p className="nota-titulo">não foi possível carregar suas candidaturas</p>
        <p className="nota-texto">{erro}</p>
      </div>
    )
  }

  const filtradas = candidaturas ? filtrarCandidaturas(candidaturas, busca) : []
  const vazio = candidaturas != null && candidaturas.length === 0

  return (
    <>
      <Resumo resumo={resumo} />

      <div className="console candidaturas-console">
        <label className="console-busca">
          <span className="sr-only">Buscar candidaturas</span>
          <span className="prompt" aria-hidden="true">
            ❯
          </span>
          <input
            type="search"
            placeholder="filtrar por vaga, empresa ou próximo passo..."
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
          />
        </label>
        {!celular && (
          <div className="abas-grupo" role="group" aria-label="Visualização">
            <button
              type="button"
              className={`aba ${visao === 'quadro' ? 'aba-ativa' : ''}`}
              aria-pressed={visao === 'quadro'}
              onClick={() => escolherVisao('quadro')}
            >
              <IconeQuadro tamanho={13} /> quadro
            </button>
            <button
              type="button"
              className={`aba ${visao === 'lista' ? 'aba-ativa' : ''}`}
              aria-pressed={visao === 'lista'}
              onClick={() => escolherVisao('lista')}
            >
              <IconeLista tamanho={13} /> lista
            </button>
          </div>
        )}
        <div className="candidaturas-acoes">
          <button type="button" className="botao" onClick={exportar} disabled={exportando || vazio} aria-busy={exportando}>
            <IconeDownload tamanho={13} /> csv
          </button>
          <button type="button" className="botao botao-primario" onClick={() => setCriandoExterna(true)}>
            <IconeMais tamanho={13} /> externa
          </button>
        </div>
      </div>

      {candidaturas == null && <p className="carregando-texto">carregando quadro...</p>}

      {vazio && (
        <div className="nota">
          <IconeMarcador tamanho={28} />
          <p className="nota-titulo">nenhuma candidatura ainda</p>
          <p className="nota-texto">
            Use "acompanhar" nas vagas da página inicial ou adicione uma vaga que você encontrou fora do site.
          </p>
          <div className="convite-acoes">
            <a className="botao botao-primario" href="/">
              explorar vagas
            </a>
            <button type="button" className="botao" onClick={() => setCriandoExterna(true)}>
              adicionar externa
            </button>
          </div>
        </div>
      )}

      {candidaturas != null && !vazio && filtradas.length === 0 && (
        <div className="nota">
          <IconeBusca tamanho={28} />
          <p className="nota-titulo">nada encontrado</p>
          <p className="nota-texto">Nenhuma candidatura combina com "{busca}".</p>
        </div>
      )}

      {filtradas.length > 0 &&
        (visao === 'quadro' ? (
          <Quadro candidaturas={filtradas} aoAbrir={abrir} aoMover={mover} aoNovaExterna={() => setCriandoExterna(true)} />
        ) : (
          <ListaCandidaturas candidaturas={filtradas} ocupada={movendo} aoAbrir={abrir} aoMover={mover} />
        ))}

      {abertaId != null && (
        <PainelCandidatura
          key={abertaId}
          id={abertaId}
          versao={versaoPainel}
          aoFechar={fechar}
          aoAtualizar={aoAtualizar}
          aoExcluir={aoExcluir}
          aoExpirar={expirar}
        />
      )}

      {criandoExterna && <ModalExterna aoCriar={aoCriar} aoFechar={() => setCriandoExterna(false)} />}

      {aviso && (
        <div className={`aviso-flutuante ${aviso.erro ? 'aviso-flutuante-erro' : ''}`} role="status">
          <span>{aviso.texto}</span>
          <button type="button" onClick={() => setAviso(null)} aria-label="Fechar aviso">
            <IconeLimpar tamanho={12} />
          </button>
        </div>
      )}
    </>
  )
}

export default function Candidaturas() {
  const { usuario, verificando } = useSessao()

  useEffect(() => {
    document.title = 'Candidaturas · Dev First Door'
  }, [])

  return (
    <div className="pagina candidaturas">
      <Cabecalho />
      <main>
        {verificando && <p className="carregando-texto">verificando sessão...</p>}
        {!verificando && !usuario && <Convite />}
        {!verificando && usuario && <Quadros key={usuario.id} />}
      </main>
    </div>
  )
}
