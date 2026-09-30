import { useEffect, useMemo, useState } from 'react'
import { ErroNaoAutenticado, chamarAdmin } from './api'

const FONTES_SIMPLES = [
  { id: 'PROGRAMATHOR', nome: 'ProgramaThor' },
  { id: 'REMOTEOK', nome: 'RemoteOK' },
]

const CAMPOS_NUMERICOS = {
  intervaloColetaMinutos: 'intervalo (minutos)',
  diasParaExpirar: 'dias para expirar',
  pausaLinkedinMs: 'pausa entre requisições do LinkedIn',
  variacaoPausaLinkedinMs: 'variação da pausa do LinkedIn',
}

// Campo apagado fica vazio enquanto a pessoa digita, em vez de virar 0 na hora.
function numeroOuVazio(texto) {
  return texto === '' ? '' : Number(texto)
}

// Enter num campo de lista não deve enviar (e salvar) o formulário inteiro.
function ignorarEnter(evento) {
  if (evento.key === 'Enter') evento.preventDefault()
}

const MOTIVO_LABEL = {
  DADOS_INCOMPLETOS: 'dados incompletos',
  NIVEL: 'nível incompatível',
  FORA_DE_TECNOLOGIA: 'fora de tecnologia',
  NAO_JAVA: 'não menciona Java',
  NAO_REMOTA: 'não remota',
}

function AlternarFonte({ id, ligada, desabilitada = false, aoMudar }) {
  return (
    <label className="config-alternar">
      <input
        type="checkbox"
        checked={ligada}
        disabled={desabilitada}
        onChange={(e) => aoMudar(id, e.target.checked)}
      />
      <span>{ligada ? 'ligada' : 'desligada'}</span>
    </label>
  )
}

function ResultadoBoard({ resultado }) {
  if (!resultado) return null
  if (!resultado.existe) {
    return <p className="board-resumo board-inexistente">board não encontrado</p>
  }
  return (
    <div className="board-resultado">
      <p className="board-resumo">
        {resultado.totalVagas} vagas · {resultado.totalAprovadas} aprovadas pelos filtros
      </p>
      {resultado.exemplosAprovados.length > 0 && (
        <div>
          <p className="board-subtitulo">aprovadas</p>
          <ul>
            {resultado.exemplosAprovados.map((vaga) => (
              <li key={vaga.link || vaga.titulo}>
                <a href={vaga.link} target="_blank" rel="noreferrer">
                  {vaga.titulo}
                </a>
              </li>
            ))}
          </ul>
        </div>
      )}
      {resultado.exemplosReprovados.length > 0 && (
        <div>
          <p className="board-subtitulo">reprovadas</p>
          <ul>
            {resultado.exemplosReprovados.map((vaga) => (
              <li key={`${vaga.link}-${vaga.motivo}`}>
                <a href={vaga.link} target="_blank" rel="noreferrer">
                  {vaga.titulo}
                </a>{' '}
                <span className="admin-fraco">· {MOTIVO_LABEL[vaga.motivo] ?? vaga.motivo}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

function TesteEmpresa({ ats, empresa, aoExpirar }) {
  const [testando, setTestando] = useState(false)
  const [resultado, setResultado] = useState(null)
  const [erro, setErro] = useState(null)

  async function testar() {
    setTestando(true)
    setErro(null)
    setResultado(null)
    try {
      setResultado(
        await chamarAdmin('/boards/testar', {
          method: 'POST',
          corpo: { ats, empresa },
        }),
      )
    } catch (e) {
      if (e instanceof ErroNaoAutenticado) aoExpirar()
      else setErro(e.message)
    } finally {
      setTestando(false)
    }
  }

  return (
    <div className="board-teste">
      <button
        type="button"
        className="admin-botao admin-botao-discreto"
        disabled={testando || empresa.trim() === ''}
        aria-busy={testando}
        onClick={testar}
      >
        {testando ? 'testando...' : 'testar'}
      </button>
      {erro && <p className="admin-mensagem admin-mensagem-erro">{erro}</p>}
      <ResultadoBoard resultado={resultado} />
    </div>
  )
}

function ListaEditavel({ titulo, valores, aoMudar, testeAts, aoExpirar }) {
  function alterar(indice, valor) {
    aoMudar(valores.map((atual, posicao) => (posicao === indice ? valor : atual)))
  }

  function remover(indice) {
    aoMudar(valores.filter((_, posicao) => posicao !== indice))
  }

  return (
    <fieldset className="config-lista">
      <legend>{titulo}</legend>
      {valores.length === 0 && <p className="admin-fraco">nenhum item configurado</p>}
      {valores.map((valor, indice) => (
        <div className="config-lista-item" key={indice}>
          <div className="config-lista-linha">
            <input
              type="text"
              value={valor}
              aria-label={`${titulo} ${indice + 1}`}
              onChange={(e) => alterar(indice, e.target.value)}
              onKeyDown={ignorarEnter}
            />
            {/* A key pelo valor descarta o resultado do teste quando a empresa da linha muda
                (edição ou remoção de uma linha acima), para não mostrá-lo ao lado de outra. */}
            {testeAts && <TesteEmpresa key={valor} ats={testeAts} empresa={valor} aoExpirar={aoExpirar} />}
            <button
              type="button"
              className="botao-limpar"
              onClick={() => remover(indice)}
              aria-label={`Remover ${valor || 'item vazio'}`}
            >
              remover
            </button>
          </div>
        </div>
      ))}
      <button type="button" className="admin-botao" onClick={() => aoMudar([...valores, ''])}>
        + adicionar
      </button>
    </fieldset>
  )
}

function SecaoFonte({ nome, fonte, configuracao, aoAlternar, children }) {
  return (
    <section className="config-fonte">
      <div className="config-fonte-cabecalho">
        <h2>{nome}</h2>
        <AlternarFonte
          id={fonte}
          ligada={configuracao.fontesLigadas[fonte]}
          desabilitada={fonte === 'LINKEDIN' && !configuracao.linkedinChaveMestraAtiva}
          aoMudar={aoAlternar}
        />
      </div>
      {children}
    </section>
  )
}

export default function Configuracao({ aoExpirar, aoMudarPendencia }) {
  const [configuracao, setConfiguracao] = useState(null)
  // Cópia do que está salvo no servidor, para saber se há alterações pendentes.
  const [salva, setSalva] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [salvando, setSalvando] = useState(false)
  const [mensagem, setMensagem] = useState(null)

  useEffect(() => {
    let cancelado = false
    chamarAdmin('/configuracao')
      .then((dados) => {
        if (cancelado) return
        setConfiguracao(dados)
        setSalva(JSON.stringify(dados))
      })
      .catch((e) => {
        if (cancelado) return
        if (e instanceof ErroNaoAutenticado) aoExpirar()
        else setMensagem({ tipo: 'erro', texto: e.message })
      })
      .finally(() => {
        if (!cancelado) setCarregando(false)
      })
    return () => {
      cancelado = true
    }
  }, [aoExpirar])

  const pendente = useMemo(
    () => configuracao != null && salva != null && JSON.stringify(configuracao) !== salva,
    [configuracao, salva],
  )

  useEffect(() => {
    aoMudarPendencia?.(pendente)
  }, [pendente, aoMudarPendencia])

  useEffect(() => () => aoMudarPendencia?.(false), [aoMudarPendencia])

  useEffect(() => {
    if (!pendente) return
    function avisar(evento) {
      evento.preventDefault()
    }
    window.addEventListener('beforeunload', avisar)
    return () => window.removeEventListener('beforeunload', avisar)
  }, [pendente])

  function alterar(campo, valor) {
    setConfiguracao((atual) => ({ ...atual, [campo]: valor }))
  }

  function alternarFonte(fonte, ligada) {
    setConfiguracao((atual) => ({
      ...atual,
      fontesLigadas: { ...atual.fontesLigadas, [fonte]: ligada },
    }))
  }

  async function salvar(evento) {
    evento.preventDefault()
    const vazios = Object.entries(CAMPOS_NUMERICOS)
      .filter(([campo]) => configuracao[campo] === '')
      .map(([, rotulo]) => rotulo)
    if (vazios.length > 0) {
      setMensagem({ tipo: 'erro', texto: `Preencha: ${vazios.join(', ')}.` })
      return
    }
    setSalvando(true)
    setMensagem(null)
    try {
      const resposta = await chamarAdmin('/configuracao', {
        method: 'PUT',
        corpo: configuracao,
      })
      setConfiguracao(resposta)
      setSalva(JSON.stringify(resposta))
      setMensagem({ tipo: 'ok', texto: 'Configuração salva. Ela valerá na próxima coleta.' })
    } catch (e) {
      if (e instanceof ErroNaoAutenticado) aoExpirar()
      else setMensagem({ tipo: 'erro', texto: e.message })
    } finally {
      setSalvando(false)
    }
  }

  if (carregando) return <p className="admin-carregando">carregando configuração...</p>
  if (!configuracao) {
    return (
      <p className="admin-mensagem admin-mensagem-erro" role="alert">
        {mensagem?.texto ?? 'Não foi possível carregar a configuração.'}
      </p>
    )
  }

  return (
    <form className="config" onSubmit={salvar}>
      <div className="admin-barra">
        <div>
          <h2 className="admin-secao-titulo">configuração</h2>
          <p className="admin-fraco">Alterações entram em vigor na coleta seguinte.</p>
        </div>
        <button type="submit" className="admin-botao admin-botao-primario" disabled={salvando || !pendente} aria-busy={salvando}>
          {salvando ? 'salvando...' : pendente ? 'salvar configuração' : 'nada a salvar'}
        </button>
      </div>

      {mensagem && (
        <p className={`admin-mensagem admin-mensagem-${mensagem.tipo}`} role="status">
          {mensagem.texto}
        </p>
      )}

      <section className="config-geral">
        <h2>coleta</h2>
        <div className="config-campos-numero">
          <label className="admin-campo">
            <span>intervalo (minutos)</span>
            <input
              type="number"
              min="30"
              value={configuracao.intervaloColetaMinutos}
              onChange={(e) => alterar('intervaloColetaMinutos', numeroOuVazio(e.target.value))}
            />
          </label>
          <label className="admin-campo">
            <span>dias para expirar</span>
            <input
              type="number"
              min="1"
              value={configuracao.diasParaExpirar}
              onChange={(e) => alterar('diasParaExpirar', numeroOuVazio(e.target.value))}
            />
          </label>
        </div>
      </section>

      <div className="config-fontes">
        <SecaoFonte
          nome="Gupy"
          fonte="GUPY"
          configuracao={configuracao}
          aoAlternar={alternarFonte}
        >
          <ListaEditavel
            titulo="termos de busca"
            valores={configuracao.termosBuscaGupy}
            aoMudar={(valor) => alterar('termosBuscaGupy', valor)}
          />
        </SecaoFonte>

        <SecaoFonte
          nome="LinkedIn"
          fonte="LINKEDIN"
          configuracao={configuracao}
          aoAlternar={alternarFonte}
        >
          {!configuracao.linkedinChaveMestraAtiva && (
            <p className="config-aviso">Defina LINKEDIN_ENABLED=true no servidor para habilitar esta fonte.</p>
          )}
          <ListaEditavel
            titulo="termos de busca"
            valores={configuracao.termosBuscaLinkedin}
            aoMudar={(valor) => alterar('termosBuscaLinkedin', valor)}
          />
          <div className="config-campos-numero">
            <label className="admin-campo">
              <span>pausa entre requisições (ms)</span>
              <input
                type="number"
                min="0"
                value={configuracao.pausaLinkedinMs}
                onChange={(e) => alterar('pausaLinkedinMs', numeroOuVazio(e.target.value))}
              />
            </label>
            <label className="admin-campo">
              <span>variação da pausa (ms)</span>
              <input
                type="number"
                min="0"
                value={configuracao.variacaoPausaLinkedinMs}
                onChange={(e) => alterar('variacaoPausaLinkedinMs', numeroOuVazio(e.target.value))}
              />
            </label>
          </div>
        </SecaoFonte>

        <SecaoFonte
          nome="Greenhouse"
          fonte="GREENHOUSE"
          configuracao={configuracao}
          aoAlternar={alternarFonte}
        >
          <ListaEditavel
            titulo="empresas"
            valores={configuracao.empresasGreenhouse}
            aoMudar={(valor) => alterar('empresasGreenhouse', valor)}
            testeAts="GREENHOUSE"
            aoExpirar={aoExpirar}
          />
        </SecaoFonte>

        <SecaoFonte
          nome="Lever"
          fonte="LEVER"
          configuracao={configuracao}
          aoAlternar={alternarFonte}
        >
          <ListaEditavel
            titulo="empresas"
            valores={configuracao.empresasLever}
            aoMudar={(valor) => alterar('empresasLever', valor)}
            testeAts="LEVER"
            aoExpirar={aoExpirar}
          />
        </SecaoFonte>

        {FONTES_SIMPLES.map((fonte) => (
          <SecaoFonte
            key={fonte.id}
            nome={fonte.nome}
            fonte={fonte.id}
            configuracao={configuracao}
            aoAlternar={alternarFonte}
          >
            <p className="admin-fraco">Esta fonte não possui parâmetros adicionais.</p>
          </SecaoFonte>
        ))}
      </div>
    </form>
  )
}
