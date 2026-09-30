import { Fragment, useEffect, useState } from 'react'
import { ErroNaoAutenticado, chamarAdmin } from './api'
import { ORIGEM_LABEL, STATUS_EXECUCAO_LABEL, formatarDataHora, formatarDuracao } from './formato'

const TAMANHO_PAGINA = 20

function somar(fontes, campo) {
  return fontes.reduce((total, fonte) => total + (fonte[campo] ?? 0), 0)
}

function DetalheFontes({ fontes }) {
  if (fontes.length === 0) {
    return <p className="admin-fraco">nenhuma fonte registrada nesta execução</p>
  }
  return (
    <table className="historico-detalhe">
      <thead>
        <tr>
          <th scope="col">fonte</th>
          <th scope="col">status</th>
          <th scope="col">duração</th>
          <th scope="col" className="num">encontradas</th>
          <th scope="col" className="num">novas</th>
          <th scope="col" className="num">expiradas</th>
          <th scope="col">erro</th>
        </tr>
      </thead>
      <tbody>
        {fontes.map((fonte) => (
          <tr key={fonte.fonte}>
            <td>{fonte.fonte}</td>
            <td>
              <span className="historico-status" data-status={fonte.status}>
                {fonte.status === 'ERRO' ? 'erro' : 'sucesso'}
              </span>
            </td>
            <td>{formatarDuracao(fonte.inicio, fonte.fim)}</td>
            <td className="num">{fonte.encontradas}</td>
            <td className="num">{fonte.novas}</td>
            <td className="num">{fonte.expiradas}</td>
            <td className="historico-erro">{fonte.mensagemErro ?? '—'}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export default function Historico({ aoExpirar }) {
  const [numeroPagina, setNumeroPagina] = useState(0)
  const [pagina, setPagina] = useState(null)
  const [erro, setErro] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [abertas, setAbertas] = useState(() => new Set())
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let cancelado = false
    chamarAdmin(`/execucoes?page=${numeroPagina}&size=${TAMANHO_PAGINA}`)
      .then((dados) => {
        if (cancelado) return
        setPagina(dados)
        setErro(null)
      })
      .catch((e) => {
        if (cancelado) return
        if (e instanceof ErroNaoAutenticado) aoExpirar()
        else setErro(e.message)
      })
      .finally(() => {
        if (!cancelado) setCarregando(false)
      })
    return () => {
      cancelado = true
    }
  }, [numeroPagina, versao, aoExpirar])

  // O "carregando" é ligado aqui, no evento, e desligado quando a resposta chega.
  function irParaPagina(numero) {
    setCarregando(true)
    setNumeroPagina(numero)
  }

  function atualizar() {
    setCarregando(true)
    setVersao((v) => v + 1)
  }

  function alternar(id) {
    setAbertas((atuais) => {
      const novas = new Set(atuais)
      if (novas.has(id)) novas.delete(id)
      else novas.add(id)
      return novas
    })
  }

  const execucoes = pagina?.content ?? []
  const totalPaginas = pagina?.totalPages ?? 0

  return (
    <section aria-label="Histórico de execuções">
      <div className="admin-barra">
        <p className="status-linha admin-status">
          {pagina ? `${pagina.totalElements} ${pagina.totalElements === 1 ? 'execução' : 'execuções'}` : ''}
        </p>
        <div className="admin-acoes">
          <button
            type="button"
            className="admin-botao"
            onClick={atualizar}
            disabled={carregando}
          >
            {carregando ? 'carregando...' : 'atualizar'}
          </button>
        </div>
      </div>

      {erro && (
        <p className="admin-mensagem admin-mensagem-erro" role="alert">
          {erro}
        </p>
      )}

      {pagina && execucoes.length === 0 && (
        <div className="nota">
          <p className="nota-titulo">nenhuma coleta registrada</p>
          <p className="nota-texto">As execuções aparecem aqui assim que a primeira coleta rodar.</p>
        </div>
      )}

      {execucoes.length > 0 && (
        <div className="historico-rolagem">
          <table className="historico" aria-busy={carregando}>
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">início</th>
                <th scope="col">origem</th>
                <th scope="col">duração</th>
                <th scope="col">status</th>
                <th scope="col" className="num">encontradas</th>
                <th scope="col" className="num">novas</th>
                <th scope="col" className="num">expiradas</th>
                <th scope="col">
                  <span className="sr-only">detalhe</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {execucoes.map((execucao) => {
                const aberta = abertas.has(execucao.id)
                return (
                  <Fragment key={execucao.id}>
                    <tr className={aberta ? 'historico-aberta' : undefined}>
                      <td className="admin-fraco">{execucao.id}</td>
                      <td>{formatarDataHora(execucao.inicio)}</td>
                      <td>{ORIGEM_LABEL[execucao.origem] ?? execucao.origem}</td>
                      <td>{execucao.fim ? formatarDuracao(execucao.inicio, execucao.fim) : '—'}</td>
                      <td>
                        <span className="historico-status" data-status={execucao.status}>
                          {STATUS_EXECUCAO_LABEL[execucao.status] ?? execucao.status}
                        </span>
                      </td>
                      <td className="num">{somar(execucao.fontes, 'encontradas')}</td>
                      <td className="num">{somar(execucao.fontes, 'novas')}</td>
                      <td className="num">{somar(execucao.fontes, 'expiradas')}</td>
                      <td>
                        <button
                          type="button"
                          className="botao-limpar historico-alternar"
                          aria-expanded={aberta}
                          onClick={() => alternar(execucao.id)}
                        >
                          {aberta ? 'fechar' : `fontes (${execucao.fontes.length})`}
                        </button>
                      </td>
                    </tr>
                    {aberta && (
                      <tr className="historico-linha-detalhe">
                        <td colSpan={9}>
                          <DetalheFontes fontes={execucao.fontes} />
                        </td>
                      </tr>
                    )}
                  </Fragment>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {totalPaginas > 1 && (
        <nav className="historico-paginacao" aria-label="Paginação do histórico">
          <button
            type="button"
            className="admin-botao"
            disabled={carregando || pagina.first}
            onClick={() => irParaPagina(pagina.number - 1)}
          >
            ← anterior
          </button>
          <span className="admin-fraco">
            página {pagina.number + 1} de {totalPaginas}
          </span>
          <button
            type="button"
            className="admin-botao"
            disabled={carregando || pagina.last}
            onClick={() => irParaPagina(pagina.number + 1)}
          >
            próxima →
          </button>
        </nav>
      )}
    </section>
  )
}
