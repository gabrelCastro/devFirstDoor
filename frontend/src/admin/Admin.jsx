import { useCallback, useEffect, useState } from 'react'
import '../App.css'
import './Admin.css'
import { useTema } from '../tema'
import { IconeLua, IconeSol } from '../icons'
import { ErroNaoAutenticado, apagarCredencial, chamarAdmin, lerCredencial } from './api'
import Login from './Login'
import Painel from './Painel'
import Historico from './Historico'
import Configuracao from './Configuracao'
import Vagas from './Vagas'
import Descartes from './Descartes'
import Metricas from './Metricas'

const TELAS = [
  { id: 'painel', rotulo: 'painel' },
  { id: 'historico', rotulo: 'histórico' },
  { id: 'configuracao', rotulo: 'configuração' },
  { id: 'vagas', rotulo: 'vagas' },
  { id: 'descartes', rotulo: 'descartes' },
  { id: 'metricas', rotulo: 'métricas' },
]

export default function Admin() {
  const [tema, alternarTema] = useTema()
  const [usuario, setUsuario] = useState(null)
  // Com credencial salva na sessão, valida antes de mostrar o login.
  const [verificando, setVerificando] = useState(() => lerCredencial() != null)
  const [tela, setTela] = useState('painel')
  const [avisoLogin, setAvisoLogin] = useState(null)
  // A tela de configuração avisa quando tem alterações não salvas, que se perderiam ao sair dela.
  const [configPendente, setConfigPendente] = useState(false)

  function podeSairDaConfiguracao() {
    return !configPendente || window.confirm('Há alterações não salvas na configuração. Descartar?')
  }

  function trocarTela(id) {
    if (id === tela || !podeSairDaConfiguracao()) return
    setTela(id)
  }

  useEffect(() => {
    if (!verificando) return
    let cancelado = false
    chamarAdmin('/me')
      .then((dados) => {
        if (!cancelado) setUsuario(dados.usuario)
      })
      .catch((e) => {
        if (cancelado) return
        if (e instanceof ErroNaoAutenticado) apagarCredencial()
        else setAvisoLogin(e.message)
      })
      .finally(() => {
        if (!cancelado) setVerificando(false)
      })
    return () => {
      cancelado = true
    }
  }, [verificando])

  const sair = useCallback(() => {
    if (configPendente && !window.confirm('Há alterações não salvas na configuração. Descartar?')) return
    apagarCredencial()
    setUsuario(null)
    setAvisoLogin(null)
  }, [configPendente])

  // Qualquer 401 no meio do uso cai aqui: volta para o login avisando o motivo.
  const expirar = useCallback(() => {
    apagarCredencial()
    setUsuario(null)
    setAvisoLogin('Sessão expirada ou credencial inválida. Entre de novo.')
  }, [])

  function entrar(nome) {
    setAvisoLogin(null)
    setUsuario(nome)
  }

  return (
    <div className="pagina admin">
      <header className="admin-cabecalho">
        <div className="admin-marca">
          <a href="/" className="admin-voltar">
            Dev First Door
          </a>
          <span className="admin-marca-sep" aria-hidden="true">
            /
          </span>
          <h1 className="admin-titulo">admin</h1>
        </div>

        <div className="admin-sessao">
          {usuario && (
            <>
              <span className="admin-usuario">{usuario}</span>
              <button type="button" className="admin-botao admin-botao-discreto" onClick={sair}>
                sair
              </button>
            </>
          )}
          <button
            type="button"
            className="botao-tema admin-botao-tema"
            onClick={alternarTema}
            aria-label={tema === 'claro' ? 'Ativar tema escuro' : 'Ativar tema claro'}
            title={tema === 'claro' ? 'Ativar tema escuro' : 'Ativar tema claro'}
          >
            {tema === 'claro' ? <IconeLua tamanho={15} /> : <IconeSol tamanho={15} />}
          </button>
        </div>
      </header>

      {verificando && <p className="admin-carregando">verificando sessão...</p>}

      {!verificando && !usuario && <Login aoEntrar={entrar} aviso={avisoLogin} />}

      {!verificando && usuario && (
        <>
          <nav className="abas-grupo admin-navegacao" aria-label="Telas da área administrativa">
            {TELAS.map(({ id, rotulo }) => (
              <button
                key={id}
                type="button"
                className={`aba ${tela === id ? 'aba-ativa' : ''}`}
                aria-current={tela === id ? 'page' : undefined}
                onClick={() => trocarTela(id)}
              >
                {rotulo}
              </button>
            ))}
          </nav>

          <main>
            {tela === 'painel' && <Painel aoExpirar={expirar} />}
            {tela === 'historico' && <Historico aoExpirar={expirar} />}
            {tela === 'configuracao' && <Configuracao aoExpirar={expirar} aoMudarPendencia={setConfigPendente} />}
            {tela === 'vagas' && <Vagas aoExpirar={expirar} />}
            {tela === 'descartes' && <Descartes aoExpirar={expirar} />}
            {tela === 'metricas' && <Metricas aoExpirar={expirar} />}
          </main>
        </>
      )}
    </div>
  )
}
