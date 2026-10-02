import { useEffect, useState } from 'react'
import { ErroNaoAutenticado, chamarAdmin } from './api'
import { formatarDataHora } from './formato'

function Paginacao({ pagina, carregando, aoMudar }) {
  if (!pagina || pagina.totalPages <= 1) return null
  return (
    <nav className="historico-paginacao" aria-label="Paginação dos usuários">
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

export default function Usuarios({ usuarioAtual, aoExpirar }) {
  const [numeroPagina, setNumeroPagina] = useState(0)
  const [versao, setVersao] = useState(0)
  const [pagina, setPagina] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [acao, setAcao] = useState(null)
  const [mensagem, setMensagem] = useState(null)

  useEffect(() => {
    let cancelado = false
    chamarAdmin(`/usuarios?page=${numeroPagina}`)
      .then((dados) => {
        if (cancelado) return
        setPagina(dados)
        setCarregando(false)
      })
      .catch((e) => {
        if (cancelado) return
        if (e instanceof ErroNaoAutenticado) aoExpirar()
        else setMensagem({ tipo: 'erro', texto: e.message })
        setCarregando(false)
      })
    return () => { cancelado = true }
  }, [numeroPagina, versao, aoExpirar])

  function mudarPagina(numero) {
    setCarregando(true)
    setNumeroPagina(numero)
  }

  async function atualizar(usuario, alteracao, confirmacao) {
    if (confirmacao && !window.confirm(confirmacao)) return
    setAcao(usuario.id)
    setMensagem(null)
    try {
      await chamarAdmin(`/usuarios/${usuario.id}`, { method: 'PATCH', corpo: alteracao })
      setCarregando(true)
      setVersao((valor) => valor + 1)
    } catch (e) {
      if (e instanceof ErroNaoAutenticado) aoExpirar()
      else setMensagem({ tipo: 'erro', texto: e.message })
    } finally {
      setAcao(null)
    }
  }

  const usuarios = pagina?.content ?? []
  return (
    <section aria-labelledby="usuarios-titulo">
      <div className="admin-barra">
        <div>
          <h2 id="usuarios-titulo" className="admin-secao-titulo">usuários</h2>
          <p className="admin-fraco">Contas cadastradas. A sua conta não pode ser alterada por aqui.</p>
        </div>
        <p className="admin-status">
          {pagina ? `${pagina.totalElements} ${pagina.totalElements === 1 ? 'conta' : 'contas'}` : ''}
        </p>
      </div>

      {mensagem && (
        <p className={`admin-mensagem admin-mensagem-${mensagem.tipo}`} role="status">{mensagem.texto}</p>
      )}
      {!pagina && carregando && <p className="admin-carregando">carregando usuários...</p>}
      {usuarios.length > 0 && (
        <div className="historico-rolagem">
          <table className="historico">
            <thead>
              <tr>
                <th scope="col">usuário</th>
                <th scope="col">papel</th>
                <th scope="col">situação</th>
                <th scope="col">criado em</th>
                <th scope="col"><span className="sr-only">ações</span></th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map((usuario) => {
                const proprio = usuario.usuario === usuarioAtual
                const ocupado = acao === usuario.id
                return (
                  <tr key={usuario.id}>
                    <td>
                      {usuario.usuario}
                      {proprio && <span className="vaga-manual">você</span>}
                    </td>
                    <td>
                      <label className="sr-only" htmlFor={`papel-${usuario.id}`}>Papel de {usuario.usuario}</label>
                      <select
                        id={`papel-${usuario.id}`}
                        className="vaga-selecao"
                        value={usuario.papel}
                        disabled={proprio || ocupado}
                        aria-busy={ocupado}
                        onChange={(e) => atualizar(
                          usuario,
                          { papel: e.target.value },
                          e.target.value === 'ADMIN'
                            ? `Dar acesso administrativo a ${usuario.usuario}?`
                            : null,
                        )}
                      >
                        <option value="USUARIO">usuário</option>
                        <option value="ADMIN">admin</option>
                      </select>
                    </td>
                    <td>
                      <span className="vaga-status" data-status={usuario.ativo ? 'ATIVA' : 'OCULTA'}>
                        {usuario.ativo ? 'ativo' : 'desativado'}
                      </span>
                    </td>
                    <td>{formatarDataHora(usuario.criadoEm)}</td>
                    <td>
                      <button
                        type="button"
                        className="admin-botao admin-botao-discreto"
                        disabled={proprio || ocupado}
                        aria-busy={ocupado}
                        onClick={() => atualizar(
                          usuario,
                          { ativo: !usuario.ativo },
                          usuario.ativo ? `Desativar ${usuario.usuario}? A conta deixa de conseguir entrar.` : null,
                        )}
                      >
                        {usuario.ativo ? 'desativar' : 'reativar'}
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
      <Paginacao pagina={pagina} carregando={carregando} aoMudar={mudarPagina} />
    </section>
  )
}
