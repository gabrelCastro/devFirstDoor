import { useEffect, useState } from 'react'
import { ErroNaoAutenticado, chamarApi } from '../conta/sessao'
import { criarVersao } from './api'
import { DESCRICAO_MAX, DESCRICAO_MIN, podeAdaptar } from './curriculoDados'

const ETAPAS_ESPERA = ['Analisando a vaga...', 'Adaptando seu currículo...', 'Conferindo cada tópico com o seu perfil...']

/** Cola a descrição (ou parte de uma candidatura) e pede a adaptação. */
export default function Adaptar({ perfil, status, candidaturaInicial, aoCriar, aoEditarPerfil, aoExpirar }) {
  const [descricao, setDescricao] = useState('')
  const [candidaturaId, setCandidaturaId] = useState(candidaturaInicial ?? '')
  const [candidaturas, setCandidaturas] = useState([])
  const [enviando, setEnviando] = useState(false)
  const [etapa, setEtapa] = useState(0)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    let cancelado = false
    chamarApi('/api/candidaturas')
      .then((lista) => {
        if (!cancelado) setCandidaturas(lista.filter((c) => !['CONTRATADO', 'REPROVADO', 'DESISTENCIA'].includes(c.etapa)))
      })
      .catch(() => {})
    return () => {
      cancelado = true
    }
  }, [])

  useEffect(() => {
    if (!enviando) return
    const temporizadores = [setTimeout(() => setEtapa(1), 5000), setTimeout(() => setEtapa(2), 14000)]
    return () => temporizadores.forEach(clearTimeout)
  }, [enviando])

  const tamanho = descricao.trim().length
  const semIa = status && !status.iaConfigurada
  const semCota = status && status.usadasNasUltimas24h >= status.limiteDiario
  const bloqueio = !podeAdaptar(perfil)
    ? 'perfil'
    : semIa ? 'ia' : semCota ? 'cota' : null

  async function enviar(evento) {
    evento.preventDefault()
    if (tamanho < DESCRICAO_MIN) {
      setErro(`Cole a descrição completa da vaga (pelo menos ${DESCRICAO_MIN} caracteres).`)
      return
    }
    setEtapa(0)
    setEnviando(true)
    setErro(null)
    try {
      aoCriar(await criarVersao(descricao, candidaturaId ? Number(candidaturaId) : null))
    } catch (e) {
      if (e instanceof ErroNaoAutenticado) aoExpirar()
      else setErro(e.message)
      setEnviando(false)
    }
  }

  if (bloqueio === 'perfil') {
    return (
      <div className="nota">
        <p className="nota-titulo">primeiro, o seu perfil</p>
        <p className="nota-texto">A IA só usa fatos do seu perfil. Cadastre pelo menos uma experiência ou projeto e salve.</p>
        <button type="button" className="botao botao-primario" onClick={aoEditarPerfil}>preencher perfil</button>
      </div>
    )
  }

  return (
    <form className="adaptar" onSubmit={enviar} noValidate>
      <div className="adaptar-explicacao">
        <h3>Adaptar para uma vaga</h3>
        <p>
          Cole a descrição da vaga. A IA analisa os requisitos e reescreve os tópicos do seu perfil com o vocabulário
          da vaga. <strong>Ela não inventa nada</strong>: cada tópico precisa apontar para algo que está no seu perfil,
          e tudo passa por uma checagem antes de você ver.
        </p>
        {status && status.iaConfigurada && (
          <p className="adaptar-cota">{status.usadasNasUltimas24h} de {status.limiteDiario} adaptações usadas nas últimas 24 horas</p>
        )}
      </div>

      {bloqueio === 'ia' && <p className="mensagem mensagem-erro">A adaptação com IA não está configurada neste servidor.</p>}
      {bloqueio === 'cota' && (
        <p className="mensagem mensagem-erro">Você usou as {status.limiteDiario} adaptações das últimas 24 horas. Tente de novo mais tarde.</p>
      )}

      {candidaturas.length > 0 && (
        <div className="campo-cv">
          <label htmlFor="adaptar-candidatura">Ligar a uma candidatura (opcional)</label>
          <select id="adaptar-candidatura" value={candidaturaId} onChange={(e) => setCandidaturaId(e.target.value)}>
            <option value="">nenhuma</option>
            {candidaturas.map((c) => (
              <option key={c.id} value={c.id}>{c.empresa} — {c.titulo}</option>
            ))}
          </select>
          <p className="campo-cv-dica">Assim você sabe qual currículo mandou para cada vaga.</p>
        </div>
      )}

      <div className="campo-cv">
        <label htmlFor="adaptar-descricao">Descrição da vaga</label>
        <textarea
          id="adaptar-descricao"
          rows={12}
          maxLength={DESCRICAO_MAX}
          value={descricao}
          placeholder="Cole aqui o texto da vaga: atividades, requisitos, diferenciais..."
          onChange={(e) => {
            setDescricao(e.target.value)
            setErro(null)
          }}
        />
        <p className="campo-cv-dica">
          {tamanho < DESCRICAO_MIN ? `mínimo de ${DESCRICAO_MIN} caracteres` : 'ok'}
          <span className="campo-cv-contador">{tamanho}/{DESCRICAO_MAX}</span>
        </p>
      </div>

      {erro && <p className="mensagem mensagem-erro" role="alert">{erro}</p>}

      <div className="adaptar-acoes">
        <button type="submit" className="botao botao-primario botao-grande" disabled={enviando || bloqueio != null} aria-busy={enviando}>
          {enviando ? 'gerando...' : 'adaptar com IA'}
        </button>
        {enviando && (
          <p className="adaptar-progresso" role="status">
            <span className="pulso" data-status="sincronizando" aria-hidden="true" />
            {ETAPAS_ESPERA[etapa]} <span className="secao-cv-ajuda">(leva de 10 a 40 segundos)</span>
          </p>
        )}
      </div>
    </form>
  )
}
