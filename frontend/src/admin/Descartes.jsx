import { useEffect, useState } from 'react'
import { ErroNaoAutenticado, chamarAdmin } from './api'
import { FONTES_ADMIN, MOTIVO_DESCARTE_LABEL, montarCaminhoDescartes } from './adminDados'
import { formatarDataHora } from './formato'

const FILTROS_INICIAIS = { fonte: '', motivo: '', busca: '' }
const MOTIVOS = Object.keys(MOTIVO_DESCARTE_LABEL)

function Filtros({ filtros, carregando, aoFiltrar }) {
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
        <span>fonte</span>
        <select value={rascunho.fonte} onChange={(e) => alterar('fonte', e.target.value)}>
          <option value="">todas</option>
          {FONTES_ADMIN.map((fonte) => <option key={fonte}>{fonte}</option>)}
        </select>
      </label>
      <label className="admin-campo">
        <span>motivo</span>
        <select value={rascunho.motivo} onChange={(e) => alterar('motivo', e.target.value)}>
          <option value="">todos</option>
          {MOTIVOS.map((motivo) => (
            <option key={motivo} value={motivo}>{MOTIVO_DESCARTE_LABEL[motivo]}</option>
          ))}
        </select>
      </label>
      <div className="vagas-filtros-acoes">
        <button type="submit" className="admin-botao admin-botao-primario" disabled={carregando} aria-busy={carregando}>
          filtrar
        </button>
        <button type="button" className="botao-limpar" onClick={limpar}>limpar</button>
      </div>
    </form>
  )
}

function Contagens({ contagens }) {
  return (
    <div className="descarte-contagens" aria-label="Contagem por motivo">
      {MOTIVOS.map((motivo) => (
        <div className="descarte-contagem" key={motivo}>
          <strong>{contagens?.[motivo] ?? 0}</strong>
          <span>{MOTIVO_DESCARTE_LABEL[motivo]}</span>
        </div>
      ))}
    </div>
  )
}

function Paginacao({ pagina, carregando, aoMudar }) {
  if (!pagina || pagina.totalPages <= 1) return null
  return (
    <nav className="historico-paginacao" aria-label="Paginação dos descartes">
      <button type="button" className="admin-botao" disabled={carregando || pagina.first} onClick={() => aoMudar(pagina.number - 1)}>
        ← anterior
      </button>
      <span className="admin-fraco">página {pagina.number + 1} de {pagina.totalPages}</span>
      <button type="button" className="admin-botao" disabled={carregando || pagina.last} onClick={() => aoMudar(pagina.number + 1)}>
        próxima →
      </button>
    </nav>
  )
}

export default function Descartes({ aoExpirar }) {
  const [filtros, setFiltros] = useState(FILTROS_INICIAIS)
  const [numeroPagina, setNumeroPagina] = useState(0)
  const [resposta, setResposta] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    let cancelado = false
    chamarAdmin(montarCaminhoDescartes(filtros, numeroPagina))
      .then((dados) => {
        if (cancelado) return
        setResposta(dados)
        setErro(null)
        setCarregando(false)
      })
      .catch((e) => {
        if (cancelado) return
        if (e instanceof ErroNaoAutenticado) aoExpirar()
        else setErro(e.message)
        setCarregando(false)
      })
    return () => { cancelado = true }
  }, [filtros, numeroPagina, aoExpirar])

  function filtrar(novosFiltros) {
    setCarregando(true)
    setNumeroPagina(0)
    setFiltros(novosFiltros)
  }

  function mudarPagina(numero) {
    setCarregando(true)
    setNumeroPagina(numero)
  }

  const pagina = resposta?.descartes
  const descartes = pagina?.content ?? []
  return (
    <section aria-labelledby="descartes-titulo">
      <div className="admin-barra">
        <div>
          <h2 id="descartes-titulo" className="admin-secao-titulo">descartes</h2>
          <p className="admin-fraco">Registros dos últimos 30 dias.</p>
        </div>
        <p className="admin-status">{pagina ? `${pagina.totalElements} encontrados` : ''}</p>
      </div>

      {resposta && <Contagens contagens={resposta.contagensPorMotivo} />}
      <Filtros filtros={filtros} carregando={carregando} aoFiltrar={filtrar} />
      {erro && <p className="admin-mensagem admin-mensagem-erro" role="alert">{erro}</p>}
      {!resposta && carregando && <p className="admin-carregando">carregando descartes...</p>}
      {pagina && descartes.length === 0 && (
        <div className="nota">
          <p className="nota-titulo">nenhum descarte encontrado</p>
          <p className="nota-texto">Ajuste os filtros para ampliar a busca.</p>
        </div>
      )}
      {descartes.length > 0 && (
        <div className="historico-rolagem">
          <table className="historico descartes-tabela">
            <thead><tr><th>vaga</th><th>fonte</th><th>motivo</th><th>data</th></tr></thead>
            <tbody>
              {descartes.map((descarte) => (
                <tr key={descarte.id}>
                  <td className="vaga-identificacao">
                    <a href={descarte.link} target="_blank" rel="noreferrer">{descarte.titulo || 'título indisponível'}</a>
                    <span>{descarte.empresa || 'empresa não informada'} · {descarte.local || 'local não informado'}</span>
                  </td>
                  <td>{descarte.fonte}</td>
                  <td><span className="descarte-motivo">{MOTIVO_DESCARTE_LABEL[descarte.motivo] ?? descarte.motivo}</span></td>
                  <td>{formatarDataHora(descarte.data)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Paginacao pagina={pagina} carregando={carregando} aoMudar={mudarPagina} />
    </section>
  )
}
