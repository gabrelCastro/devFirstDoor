import { useCallback, useEffect, useState } from 'react'
import '../App.css'
import '../conta/conta.css'
import '../paginaInterna.css'
import './Curriculo.css'
import { useTema } from '../tema'
import { IconeLua, IconeSol } from '../icons'
import MenuConta from '../conta/MenuConta'
import { useSessao } from '../conta/useSessao'
import { ErroNaoAutenticado } from '../conta/sessao'
import { obterPerfil, obterStatus, obterVersao } from './api'
import { prepararPerfil } from './curriculoDados'
import EditorPerfil from './EditorPerfil'
import Adaptar from './Adaptar'
import Versoes from './Versoes'
import ResultadoVersao from './ResultadoVersao'

const ABAS = [
  { id: 'perfil', rotulo: 'meu perfil' },
  { id: 'adaptar', rotulo: 'adaptar para vaga' },
  { id: 'versoes', rotulo: 'versões' },
]

function lerUrl() {
  const params = new URLSearchParams(window.location.search)
  const aba = ABAS.some((a) => a.id === params.get('aba')) ? params.get('aba') : 'perfil'
  const numero = (nome) => {
    const n = Number(params.get(nome))
    return Number.isInteger(n) && n > 0 ? n : null
  }
  return { aba, versao: numero('versao'), candidatura: numero('candidatura') }
}

function escreverUrl({ aba, versao, candidatura }) {
  const url = new URL(window.location.href)
  url.search = ''
  if (aba !== 'perfil') url.searchParams.set('aba', aba)
  if (versao) url.searchParams.set('versao', versao)
  if (candidatura) url.searchParams.set('candidatura', candidatura)
  window.history.replaceState(null, '', url)
}

function Cabecalho() {
  const [tema, alternarTema] = useTema()
  return (
    <header className="topo">
      <div className="topo-marca">
        <a href="/" className="topo-voltar">Dev First Door</a>
        <span className="topo-sep" aria-hidden="true">/</span>
        <h1 className="topo-titulo">currículo</h1>
      </div>
      <div className="cabecalho-acoes-linha">
        <MenuConta paginaAtual="curriculo" />
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

function AreaCurriculo() {
  const { sair } = useSessao()
  const [url, setUrl] = useState(lerUrl)
  const [perfil, setPerfil] = useState(null)
  const [status, setStatus] = useState(null)
  const [versao, setVersao] = useState(null)
  const [erro, setErro] = useState(null)

  const expirar = useCallback(() => {
    sair()
  }, [sair])

  const navegar = useCallback((novo) => {
    setUrl((atual) => {
      const proximo = { ...atual, ...novo }
      escreverUrl(proximo)
      return proximo
    })
  }, [])

  const atualizarStatus = useCallback(() => {
    obterStatus().then(setStatus).catch(() => {})
  }, [])

  useEffect(() => {
    let cancelado = false
    Promise.all([obterPerfil(), obterStatus()])
      .then(([p, s]) => {
        if (cancelado) return
        setPerfil(prepararPerfil(p))
        setStatus(s)
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

  // Versão aberta pela URL (?versao=): carrega quando o id muda.
  useEffect(() => {
    if (!url.versao || versao?.id === url.versao) return
    let cancelado = false
    obterVersao(url.versao)
      .then((v) => {
        if (!cancelado) setVersao(v)
      })
      .catch((e) => {
        if (cancelado) return
        if (e instanceof ErroNaoAutenticado) expirar()
        else navegar({ versao: null })
      })
    return () => {
      cancelado = true
    }
  }, [url.versao, versao?.id, expirar, navegar])

  if (erro) return <p className="mensagem mensagem-erro" role="alert">{erro}</p>
  if (!perfil) return <p className="carregando-texto">carregando seu currículo...</p>

  const versaoAberta = url.versao && versao?.id === url.versao ? versao : null

  return (
    <>
      <nav className="abas-grupo abas-cv" aria-label="Seções do currículo">
        {ABAS.map((aba) => (
          <button
            key={aba.id}
            type="button"
            className={`aba ${url.aba === aba.id ? 'aba-ativa' : ''}`}
            aria-current={url.aba === aba.id ? 'page' : undefined}
            onClick={() => navegar({ aba: aba.id, versao: null })}
          >
            {aba.rotulo}
          </button>
        ))}
      </nav>

      {url.aba === 'perfil' && (
        <EditorPerfil perfilInicial={perfil} aoSalvar={setPerfil} aoExpirar={expirar} />
      )}

      {url.aba === 'adaptar' && (
        <Adaptar
          perfil={perfil}
          status={status}
          candidaturaInicial={url.candidatura}
          aoCriar={(nova) => {
            setVersao(nova)
            atualizarStatus()
            navegar({ aba: 'versoes', versao: nova.id, candidatura: null })
          }}
          aoEditarPerfil={() => navegar({ aba: 'perfil' })}
          aoExpirar={expirar}
        />
      )}

      {url.aba === 'versoes' && !url.versao && (
        <Versoes
          aoAbrir={(id) => navegar({ versao: id })}
          aoNova={() => navegar({ aba: 'adaptar' })}
          aoExpirar={expirar}
        />
      )}

      {url.aba === 'versoes' && url.versao && (
        versaoAberta ? (
          <>
            <button type="button" className="botao-limpar voltar-lista" onClick={() => navegar({ versao: null })}>
              ← todas as versões
            </button>
            <ResultadoVersao
              key={versaoAberta.id}
              versao={versaoAberta}
              aoAtualizar={setVersao}
              aoEditarPerfil={() => navegar({ aba: 'perfil', versao: null })}
              aoExpirar={expirar}
            />
          </>
        ) : (
          <p className="carregando-texto">carregando versão...</p>
        )
      )}
    </>
  )
}

function Convite() {
  const { pedirLogin } = useSessao()
  return (
    <div className="convite">
      <p className="nota-titulo">seu currículo, sob medida para cada vaga</p>
      <p className="nota-texto">
        Preencha seu perfil uma vez. Para cada vaga, a IA reescreve os tópicos com o vocabulário dela, sem inventar
        nada, e você baixa em PDF ou DOCX pronto para os sistemas de triagem.
      </p>
      <div className="convite-acoes">
        <button type="button" className="botao botao-primario" onClick={() => pedirLogin({ aba: 'criar' })}>criar conta</button>
        <button type="button" className="botao" onClick={() => pedirLogin()}>já tenho conta</button>
      </div>
    </div>
  )
}

export default function Curriculo() {
  const { usuario, verificando } = useSessao()

  useEffect(() => {
    document.title = 'Currículo · Dev First Door'
  }, [])

  return (
    <div className="pagina curriculo">
      <Cabecalho />
      <main>
        {verificando && <p className="carregando-texto">verificando sessão...</p>}
        {!verificando && !usuario && <Convite />}
        {!verificando && usuario && <AreaCurriculo key={usuario.id} />}
      </main>
    </div>
  )
}
