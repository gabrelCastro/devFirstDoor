import { useEffect, useState } from 'react'
import { ErroNaoAutenticado } from '../conta/sessao'
import { excluirVersao, listarVersoes } from './api'

function formatarData(iso) {
  const data = new Date(iso.replace(/(\.\d{3})\d+/, '$1'))
  return Number.isNaN(data.getTime()) ? '—' : data.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
}

/** Histórico de currículos adaptados, com o ganho de cobertura de cada um. */
export default function Versoes({ aoAbrir, aoNova, aoExpirar }) {
  const [versoes, setVersoes] = useState(null)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    let cancelado = false
    listarVersoes()
      .then((lista) => {
        if (!cancelado) setVersoes(lista)
      })
      .catch((e) => {
        if (cancelado) return
        if (e instanceof ErroNaoAutenticado) aoExpirar()
        else setErro(e.message)
      })
    return () => {
      cancelado = true
    }
  }, [aoExpirar])

  async function excluir(versao) {
    if (!window.confirm(`Excluir o currículo para "${versao.titulo}"?`)) return
    try {
      await excluirVersao(versao.id)
      setVersoes((atuais) => atuais.filter((v) => v.id !== versao.id))
    } catch (e) {
      if (e instanceof ErroNaoAutenticado) aoExpirar()
      else setErro(e.message)
    }
  }

  if (erro) return <p className="mensagem mensagem-erro" role="alert">{erro}</p>
  if (!versoes) return <p className="carregando-texto">carregando versões...</p>
  if (versoes.length === 0) {
    return (
      <div className="nota">
        <p className="nota-titulo">nenhum currículo adaptado ainda</p>
        <p className="nota-texto">Cada adaptação fica guardada aqui, para você baixar de novo quando precisar.</p>
        <button type="button" className="botao botao-primario" onClick={aoNova}>adaptar para uma vaga</button>
      </div>
    )
  }
  return (
    <ul className="versoes">
      {versoes.map((v) => (
        <li key={v.id} className="versao">
          <button type="button" className="versao-abrir" onClick={() => aoAbrir(v.id)}>
            <strong>{v.titulo}</strong>
            <span>
              {formatarData(v.criadaEm)}
              {v.candidaturaId && ' · ligado a uma candidatura'}
            </span>
          </button>
          <span className="versao-cobertura" title="Palavras-chave da vaga no currículo">
            {v.coberturaAntes} → <strong>{v.coberturaDepois}</strong>/{v.palavrasChave}
          </span>
          <button type="button" className="botao-limpar" onClick={() => excluir(v)}>excluir</button>
        </li>
      ))}
    </ul>
  )
}
