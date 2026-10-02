import { useState } from 'react'
import Dialogo from '../conta/Dialogo'
import { criarCandidatura } from './api'
import { ETAPAS } from './candidaturasDados'

function hojeIso() {
  const agora = new Date()
  return new Date(agora.getTime() - agora.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}

/** Candidatura de uma vaga que o site não coletou (indicação, site da empresa, LinkedIn...). */
export default function ModalExterna({ aoCriar, aoFechar }) {
  const [dados, setDados] = useState({ titulo: '', empresa: '', local: '', link: '', etapa: 'CANDIDATADO', dataCandidatura: hojeIso() })
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState(null)
  const enviada = dados.etapa !== 'INTERESSE' && dados.etapa !== 'DESISTENCIA'

  function alterar(campo, valor) {
    setDados((atual) => ({ ...atual, [campo]: valor }))
  }

  async function enviar(evento) {
    evento.preventDefault()
    if (!dados.titulo.trim() || !dados.empresa.trim()) {
      setErro('Informe pelo menos a vaga e a empresa.')
      return
    }
    setEnviando(true)
    setErro(null)
    try {
      const criada = await criarCandidatura({
        titulo: dados.titulo,
        empresa: dados.empresa,
        local: dados.local || null,
        link: dados.link || null,
        etapa: dados.etapa,
        dataCandidatura: enviada && dados.dataCandidatura ? dados.dataCandidatura : null,
      })
      aoCriar(criada.candidatura)
    } catch (e) {
      setErro(e.message)
      setEnviando(false)
    }
  }

  return (
    <Dialogo
      titulo="Nova candidatura externa"
      subtitulo="Para vagas que você encontrou fora do Dev First Door."
      aoFechar={aoFechar}
      className="dialogo-largo"
    >
      <form className="formulario" onSubmit={enviar} noValidate>
        <label className="campo">
          <span>vaga *</span>
          <input type="text" maxLength={255} value={dados.titulo} onChange={(e) => alterar('titulo', e.target.value)} />
        </label>
        <div className="formulario-dupla">
          <label className="campo">
            <span>empresa *</span>
            <input type="text" maxLength={255} value={dados.empresa} onChange={(e) => alterar('empresa', e.target.value)} />
          </label>
          <label className="campo">
            <span>local</span>
            <input
              type="text"
              maxLength={255}
              placeholder="remoto, São Paulo..."
              value={dados.local}
              onChange={(e) => alterar('local', e.target.value)}
            />
          </label>
        </div>
        <label className="campo">
          <span>link</span>
          <input
            type="url"
            maxLength={1024}
            placeholder="https://"
            value={dados.link}
            onChange={(e) => alterar('link', e.target.value)}
          />
        </label>
        <div className="formulario-dupla">
          <label className="campo">
            <span>etapa</span>
            <select value={dados.etapa} onChange={(e) => alterar('etapa', e.target.value)}>
              {ETAPAS.map((etapa) => (
                <option key={etapa.id} value={etapa.id}>
                  {etapa.rotulo}
                </option>
              ))}
            </select>
          </label>
          {enviada && (
            <label className="campo">
              <span>enviada em</span>
              <input
                type="date"
                max={hojeIso()}
                value={dados.dataCandidatura}
                onChange={(e) => alterar('dataCandidatura', e.target.value)}
              />
            </label>
          )}
        </div>
        {erro && (
          <p className="mensagem mensagem-erro" role="alert">
            {erro}
          </p>
        )}
        <button type="submit" className="botao botao-primario" disabled={enviando} aria-busy={enviando}>
          {enviando ? 'salvando...' : 'adicionar ao quadro'}
        </button>
      </form>
    </Dialogo>
  )
}
