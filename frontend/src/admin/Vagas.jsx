import { useEffect, useState } from 'react'
import { ErroNaoAutenticado, chamarAdmin } from './api'
import { formatarDataHora } from './formato'
import {
  FONTES_VAGAS,
  NIVEL_VAGA_LABEL,
  STATUS_VAGA_LABEL,
  montarCaminhoVagas,
} from './vagasDados'

const FILTROS_INICIAIS = { status: '', fonte: '', busca: '' }

function Paginacao({ pagina, carregando, aoMudar }) {
  if (!pagina || pagina.totalPages <= 1) return null
  return (
    <nav className="historico-paginacao" aria-label="Paginação das vagas">
      <button
        type="button"
        className="admin-botao"
        disabled={carregando || pagina.first}
        onClick={() => aoMudar(pagina.number - 1)}
      >
        ← anterior
      </button>
      <span className="admin-fraco">
        página {pagina.number + 1} de {pagina.totalPages}
      </span>
      <button
        type="button"
        className="admin-botao"
        disabled={carregando || pagina.last}
        onClick={() => aoMudar(pagina.number + 1)}
      >
        próxima →
      </button>
    </nav>
  )
}

function FiltrosVagas({ filtros, aoFiltrar, carregando }) {
  const [rascunho, setRascunho] = useState(filtros)

  function alterar(campo, valor) {
    setRascunho((atual) => ({ ...atual, [campo]: valor }))
  }

  function enviar(evento) {
    evento.preventDefault()
    aoFiltrar(rascunho)
  }

  function limpar() {
    setRascunho(FILTROS_INICIAIS)
    aoFiltrar(FILTROS_INICIAIS)
  }

  return (
    <form className="vagas-filtros" onSubmit={enviar}>
      <label className="admin-campo vagas-busca">
        <span>busca</span>
        <input
          type="search"
          value={rascunho.busca}
          placeholder="título, empresa ou local"
          onChange={(e) => alterar('busca', e.target.value)}
        />
      </label>
      <label className="admin-campo">
        <span>status</span>
        <select value={rascunho.status} onChange={(e) => alterar('status', e.target.value)}>
          <option value="">todos</option>
          <option value="ATIVA">ativas</option>
          <option value="EXPIRADA">expiradas</option>
          <option value="OCULTA">ocultas</option>
        </select>
      </label>
      <label className="admin-campo">
        <span>fonte</span>
        <select value={rascunho.fonte} onChange={(e) => alterar('fonte', e.target.value)}>
          <option value="">todas</option>
          {FONTES_VAGAS.map((fonte) => (
            <option key={fonte} value={fonte}>
              {fonte}
            </option>
          ))}
        </select>
      </label>
      <div className="vagas-filtros-acoes">
        <button type="submit" className="admin-botao admin-botao-primario" disabled={carregando}>
          filtrar
        </button>
        <button type="button" className="botao-limpar" onClick={limpar}>
          limpar
        </button>
      </div>
    </form>
  )
}

function TabelaVagas({ vagas, acao, aoAtualizar }) {
  return (
    <div className="historico-rolagem">
      <table className="historico vagas-tabela">
        <thead>
          <tr>
            <th scope="col">vaga</th>
            <th scope="col">fonte</th>
            <th scope="col">status</th>
            <th scope="col">nível</th>
            <th scope="col">modalidade</th>
            <th scope="col">última visita</th>
            <th scope="col"><span className="sr-only">ações</span></th>
          </tr>
        </thead>
        <tbody>
          {vagas.map((vaga) => {
            const ocupada = acao === vaga.id
            return (
              <tr key={vaga.id}>
                <td className="vaga-identificacao">
                  <a href={vaga.link} target="_blank" rel="noreferrer">{vaga.titulo}</a>
                  <span>{vaga.empresa} · {vaga.local}</span>
                </td>
                <td>{vaga.fonte}</td>
                <td>
                  <span className="vaga-status" data-status={vaga.status}>
                    {STATUS_VAGA_LABEL[vaga.status] ?? vaga.status}
                  </span>
                </td>
                <td>
                  <label className="sr-only" htmlFor={`nivel-${vaga.id}`}>Nível de {vaga.titulo}</label>
                  <select
                    id={`nivel-${vaga.id}`}
                    className="vaga-selecao"
                    value={vaga.nivel}
                    disabled={ocupada}
                    onChange={(e) => aoAtualizar(vaga.id, { nivel: e.target.value })}
                  >
                    <option value="ESTAGIO">estágio</option>
                    <option value="JUNIOR">júnior</option>
                  </select>
                  {vaga.nivelManual && <span className="vaga-manual">manual</span>}
                </td>
                <td>
                  <label className="sr-only" htmlFor={`remoto-${vaga.id}`}>Modalidade de {vaga.titulo}</label>
                  <select
                    id={`remoto-${vaga.id}`}
                    className="vaga-selecao"
                    value={vaga.remoto ? 'true' : 'false'}
                    disabled={ocupada}
                    onChange={(e) => aoAtualizar(vaga.id, { remoto: e.target.value === 'true' })}
                  >
                    <option value="true">remota</option>
                    <option value="false">não remota</option>
                  </select>
                  {vaga.remotoManual && <span className="vaga-manual">manual</span>}
                </td>
                <td>{formatarDataHora(vaga.dataUltimaVisita)}</td>
                <td>
                  <button
                    type="button"
                    className="admin-botao admin-botao-discreto"
                    disabled={ocupada}
                    onClick={() =>
                      aoAtualizar(vaga.id, { status: vaga.status === 'ATIVA' ? 'OCULTA' : 'ATIVA' })
                    }
                  >
                    {ocupada ? 'salvando...' : vaga.status === 'ATIVA' ? 'ocultar' : 'reativar'}
                  </button>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

function ListaVagas({ aoExpirar }) {
  const [filtros, setFiltros] = useState(FILTROS_INICIAIS)
  const [numeroPagina, setNumeroPagina] = useState(0)
  const [pagina, setPagina] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [acao, setAcao] = useState(null)
  const [mensagem, setMensagem] = useState(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let cancelado = false
    chamarAdmin(montarCaminhoVagas(filtros, numeroPagina))
      .then((dados) => {
        if (cancelado) return
        // Ocultar a última vaga da última página (com filtro de status) esvazia essa
        // página: volta para a última que ainda existe em vez de mostrar "página 3 de 2".
        if ((dados.content ?? []).length === 0 && dados.number > 0) {
          setNumeroPagina(Math.max(0, dados.totalPages - 1))
          return
        }
        setPagina(dados)
        // Só limpa erros: a confirmação de "reclassificar" chega junto com esta recarga.
        setMensagem((atual) => (atual?.tipo === 'erro' ? null : atual))
        setCarregando(false)
      })
      .catch((e) => {
        if (cancelado) return
        if (e instanceof ErroNaoAutenticado) aoExpirar()
        else setMensagem({ tipo: 'erro', texto: e.message })
        setCarregando(false)
      })
    return () => {
      cancelado = true
    }
  }, [filtros, numeroPagina, versao, aoExpirar])

  function filtrar(novosFiltros) {
    setCarregando(true)
    setNumeroPagina(0)
    setFiltros(novosFiltros)
  }

  function mudarPagina(numero) {
    setCarregando(true)
    setNumeroPagina(numero)
  }

  async function atualizar(id, alteracao) {
    setAcao(id)
    setMensagem(null)
    try {
      await chamarAdmin(`/vagas/${id}`, { method: 'PATCH', corpo: alteracao })
      setCarregando(true)
      setVersao((valor) => valor + 1)
    } catch (e) {
      if (e instanceof ErroNaoAutenticado) aoExpirar()
      else setMensagem({ tipo: 'erro', texto: e.message })
    } finally {
      setAcao(null)
    }
  }

  async function reclassificar() {
    if (!window.confirm('Reclassificar todas as vagas? Correções manuais serão preservadas.')) return
    setAcao('reclassificar')
    setMensagem(null)
    try {
      const resposta = await chamarAdmin('/vagas/reclassificar', { method: 'POST' })
      setMensagem({ tipo: 'ok', texto: `${resposta.reclassificadas} vagas reclassificadas.` })
      setCarregando(true)
      setVersao((valor) => valor + 1)
    } catch (e) {
      if (e instanceof ErroNaoAutenticado) aoExpirar()
      else setMensagem({ tipo: 'erro', texto: e.message })
    } finally {
      setAcao(null)
    }
  }

  const vagas = pagina?.content ?? []
  return (
    <>
      <div className="admin-barra">
        <p className="admin-status">
          {pagina ? `${pagina.totalElements} ${pagina.totalElements === 1 ? 'vaga' : 'vagas'}` : ''}
        </p>
        <button
          type="button"
          className="admin-botao"
          disabled={acao != null}
          onClick={reclassificar}
        >
          {acao === 'reclassificar' ? 'reclassificando...' : 'reclassificar tudo'}
        </button>
      </div>
      <FiltrosVagas filtros={filtros} aoFiltrar={filtrar} carregando={carregando} />
      {mensagem && (
        <p className={`admin-mensagem admin-mensagem-${mensagem.tipo}`} role="status">
          {mensagem.texto}
        </p>
      )}
      {pagina && vagas.length === 0 && (
        <div className="nota">
          <p className="nota-titulo">nenhuma vaga encontrada</p>
          <p className="nota-texto">Ajuste os filtros para ampliar a busca.</p>
        </div>
      )}
      {vagas.length > 0 && <TabelaVagas vagas={vagas} acao={acao} aoAtualizar={atualizar} />}
      <Paginacao pagina={pagina} carregando={carregando} aoMudar={mudarPagina} />
    </>
  )
}

function Duplicatas({ aoExpirar }) {
  const [grupos, setGrupos] = useState(null)
  const [acao, setAcao] = useState(null)
  const [mensagem, setMensagem] = useState(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let cancelado = false
    chamarAdmin('/vagas/duplicatas')
      .then((dados) => {
        if (!cancelado) setGrupos(dados)
      })
      .catch((e) => {
        if (cancelado) return
        if (e instanceof ErroNaoAutenticado) aoExpirar()
        else setMensagem({ tipo: 'erro', texto: e.message })
      })
    return () => {
      cancelado = true
    }
  }, [versao, aoExpirar])

  async function manter(vaga) {
    setAcao(vaga.id)
    setMensagem(null)
    try {
      const resposta = await chamarAdmin('/vagas/duplicatas/resolver', {
        method: 'POST',
        corpo: { vagaMantidaId: vaga.id },
      })
      setMensagem({
        tipo: 'ok',
        texto: `${resposta.ocultadas} ${resposta.ocultadas === 1 ? 'duplicata ocultada' : 'duplicatas ocultadas'}.`,
      })
      setVersao((valor) => valor + 1)
    } catch (e) {
      if (e instanceof ErroNaoAutenticado) aoExpirar()
      else setMensagem({ tipo: 'erro', texto: e.message })
    } finally {
      setAcao(null)
    }
  }

  if (!grupos) {
    return mensagem?.tipo === 'erro' ? (
      <p className="admin-mensagem admin-mensagem-erro" role="alert">{mensagem.texto}</p>
    ) : (
      <p className="admin-carregando">procurando duplicatas...</p>
    )
  }
  return (
    <>
      <div className="admin-barra">
        <p className="admin-status">
          {grupos.length} {grupos.length === 1 ? 'grupo encontrado' : 'grupos encontrados'}
        </p>
      </div>
      {mensagem && (
        <p className={`admin-mensagem admin-mensagem-${mensagem.tipo}`} role="status">
          {mensagem.texto}
        </p>
      )}
      {grupos.length === 0 && (
        <div className="nota">
          <p className="nota-titulo">nenhuma duplicata ativa</p>
          <p className="nota-texto">Só aparecem aqui vagas iguais encontradas em fontes diferentes.</p>
        </div>
      )}
      <div className="duplicatas">
        {grupos.map((grupo) => (
          <section
            className="duplicata-grupo"
            key={`${grupo.tituloNormalizado}-${grupo.empresaNormalizada}`}
          >
            <div className="duplicata-cabecalho">
              <h3>{grupo.vagas[0]?.titulo}</h3>
              <span>{grupo.vagas[0]?.empresa}</span>
            </div>
            <ul>
              {grupo.vagas.map((vaga) => (
                <li key={vaga.id}>
                  <div>
                    <a href={vaga.link} target="_blank" rel="noreferrer">{vaga.fonte}</a>
                    <span>{vaga.local} · {NIVEL_VAGA_LABEL[vaga.nivel] ?? vaga.nivel}</span>
                  </div>
                  <button
                    type="button"
                    className="admin-botao admin-botao-discreto"
                    disabled={acao != null}
                    onClick={() => manter(vaga)}
                  >
                    {acao === vaga.id ? 'resolvendo...' : 'manter esta'}
                  </button>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>
    </>
  )
}

export default function Vagas({ aoExpirar }) {
  const [visao, setVisao] = useState('lista')
  return (
    <section aria-label="Moderação de vagas">
      <nav className="abas-grupo vagas-navegacao" aria-label="Visões da moderação">
        <button
          type="button"
          className={`aba ${visao === 'lista' ? 'aba-ativa' : ''}`}
          onClick={() => setVisao('lista')}
        >
          todas as vagas
        </button>
        <button
          type="button"
          className={`aba ${visao === 'duplicatas' ? 'aba-ativa' : ''}`}
          onClick={() => setVisao('duplicatas')}
        >
          duplicatas
        </button>
      </nav>
      {visao === 'lista' ? <ListaVagas aoExpirar={aoExpirar} /> : <Duplicatas aoExpirar={aoExpirar} />}
    </section>
  )
}
