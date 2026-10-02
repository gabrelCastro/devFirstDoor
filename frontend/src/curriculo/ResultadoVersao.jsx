import { useState } from 'react'
import { IconeAlerta, IconeDownload } from '../icons'
import { ErroNaoAutenticado } from '../conta/sessao'
import { baixarVersao, salvarEscolhas } from './api'
import { alternarRecusa, destacar, palavrasFaltando, resumoDaProposta } from './curriculoDados'

function TextoDestacado({ texto, termos }) {
  return destacar(texto, termos).map((pedaco, i) =>
    pedaco.destaque ? <mark key={i}>{pedaco.texto}</mark> : <span key={i}>{pedaco.texto}</span>,
  )
}

function Cobertura({ cobertura }) {
  const total = cobertura.palavrasChave.length
  const faltando = palavrasFaltando(cobertura)
  if (total === 0) return null
  const pct = (n) => `${Math.round((n / total) * 100)}%`
  return (
    <section className="cobertura" aria-label="Palavras-chave da vaga no currículo">
      <div className="cobertura-numeros">
        <div>
          <span className="cobertura-rotulo">antes</span>
          <strong>{cobertura.antes.length}<small>/{total}</small></strong>
        </div>
        <span className="cobertura-seta" aria-hidden="true">→</span>
        <div>
          <span className="cobertura-rotulo">depois</span>
          <strong className="cobertura-depois">{cobertura.depois.length}<small>/{total}</small></strong>
        </div>
        <p className="cobertura-explicacao">palavras-chave da vaga que o ATS vai encontrar no seu currículo</p>
      </div>
      <div className="cobertura-barra" aria-hidden="true">
        <span className="cobertura-barra-antes" style={{ width: pct(cobertura.antes.length) }} />
        <span className="cobertura-barra-depois" style={{ width: pct(cobertura.depois.length) }} />
      </div>
      <ul className="palavras">
        {cobertura.palavrasChave.map((p) => (
          <li key={p} data-presente={cobertura.depois.includes(p) || undefined}>{p}</li>
        ))}
      </ul>
      {faltando.length > 0 && (
        <p className="cobertura-faltando">
          Faltam: {faltando.join(', ')}. Só entram se você tiver essa experiência; veja as perguntas abaixo.
        </p>
      )}
    </section>
  )
}

/** Proposta da IA para uma vaga: aceitar/recusar cada tópico, lacunas, perguntas e downloads. */
export default function ResultadoVersao({ versao, aoAtualizar, aoEditarPerfil, aoExpirar }) {
  const [escolhas, setEscolhas] = useState(versao.escolhas)
  const [erro, setErro] = useState(null)
  const [baixando, setBaixando] = useState(null)
  const { proposta } = versao
  const contagem = resumoDaProposta(proposta)
  const recusados = new Set(escolhas.recusados)

  async function aplicar(novas) {
    const anteriores = escolhas
    setEscolhas(novas)
    setErro(null)
    try {
      aoAtualizar(await salvarEscolhas(versao.id, novas))
    } catch (e) {
      setEscolhas(anteriores)
      if (e instanceof ErroNaoAutenticado) aoExpirar()
      else setErro(e.message)
    }
  }

  async function baixar(formato) {
    setBaixando(formato)
    setErro(null)
    try {
      await baixarVersao(versao.id, formato)
    } catch (e) {
      if (e instanceof ErroNaoAutenticado) aoExpirar()
      else setErro(e.message)
    } finally {
      setBaixando(null)
    }
  }

  const blocos = [
    ...proposta.experiencias.map((b) => ({ ...b, tipo: 'experiência' })),
    ...proposta.projetos.map((b) => ({ ...b, tipo: 'projeto' })),
  ]

  return (
    <div className="resultado">
      <header className="resultado-cabecalho">
        <div>
          <p className="resultado-kicker">currículo para</p>
          <h3 className="resultado-titulo">{versao.titulo}</h3>
          <p className="secao-cv-ajuda">
            {contagem.total} tópicos reescritos
            {contagem.bloqueados > 0 && ` · ${contagem.bloqueados} barrados pela checagem (mantido o original)`}
          </p>
        </div>
        <div className="resultado-downloads">
          <button type="button" className="botao botao-primario" onClick={() => baixar('pdf')} disabled={baixando != null} aria-busy={baixando === 'pdf'}>
            <IconeDownload tamanho={12} /> baixar pdf
          </button>
          <button type="button" className="botao" onClick={() => baixar('docx')} disabled={baixando != null} aria-busy={baixando === 'docx'}>
            <IconeDownload tamanho={12} /> docx
          </button>
        </div>
      </header>

      {erro && <p className="mensagem mensagem-erro" role="alert">{erro}</p>}

      <Cobertura cobertura={proposta.cobertura} />

      <p className="resultado-instrucao">
        Revise cada tópico. O que você recusar volta ao texto original do seu perfil. Os destaques mostram as
        palavras da vaga.
      </p>

      {proposta.resumo?.proposto && (
        <section className="diff-bloco" aria-label="Resumo">
          <h4>Resumo</h4>
          <Item
            texto={proposta.resumo.proposto}
            originais={proposta.resumo.original ? [proposta.resumo.original] : []}
            termos={proposta.cobertura.palavrasChave}
            status={proposta.resumo.status}
            motivo={proposta.resumo.motivo}
            aceito={escolhas.usarResumo}
            aoAlternar={() => aplicar({ ...escolhas, usarResumo: !escolhas.usarResumo })}
            rotuloOriginal={proposta.resumo.original ? 'resumo atual' : 'sem resumo no perfil'}
          />
        </section>
      )}

      {blocos.map((bloco) => (
        <section key={bloco.id} className="diff-bloco" aria-label={bloco.titulo}>
          <h4>
            {bloco.titulo} <span className="diff-tipo">{bloco.tipo}</span>
          </h4>
          {bloco.bullets.length === 0 && <p className="secao-cv-ajuda">Mantidos os tópicos originais.</p>}
          {bloco.bullets.map((b) => (
            <Item
              key={b.chave}
              texto={b.texto}
              originais={b.originais}
              termos={proposta.cobertura.palavrasChave}
              status={b.status}
              motivo={b.motivo}
              aceito={!recusados.has(b.chave)}
              aoAlternar={() => aplicar(alternarRecusa(escolhas, b.chave))}
              rotuloOriginal="original"
            />
          ))}
        </section>
      ))}

      {(proposta.lacunas.length > 0 || proposta.perguntas.length > 0) && (
        <div className="sugestoes">
          {proposta.perguntas.length > 0 && (
            <section className="sugestoes-bloco" aria-labelledby="perguntas-titulo">
              <h4 id="perguntas-titulo">Você tem isso e não escreveu?</h4>
              <p className="secao-cv-ajuda">Se a resposta for sim, acrescente ao seu perfil e gere de novo: aí a IA pode usar.</p>
              <ul>
                {proposta.perguntas.map((p) => (
                  <li key={p.requisitoId + p.pergunta}>
                    <strong>{p.pergunta}</strong>
                    <span>a vaga pede: {p.requisito}</span>
                  </li>
                ))}
              </ul>
              <button type="button" className="botao" onClick={aoEditarPerfil}>editar meu perfil</button>
            </section>
          )}
          {proposta.lacunas.length > 0 && (
            <section className="sugestoes-bloco" aria-labelledby="lacunas-titulo">
              <h4 id="lacunas-titulo">O que a vaga pede e você ainda não tem</h4>
              <ul>
                {proposta.lacunas.map((l) => (
                  <li key={l.requisitoId}>
                    <strong>{l.requisito}</strong>
                    <span>{l.sugestao}</span>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </div>
      )}
    </div>
  )
}

function Item({ texto, originais, termos, status, motivo, aceito, aoAlternar, rotuloOriginal }) {
  const bloqueado = status === 'BLOQUEADO'
  const usando = !bloqueado && aceito
  return (
    <div className="diff-item" data-estado={bloqueado ? 'bloqueado' : usando ? 'aceito' : 'recusado'}>
      <div className="diff-proposto">
        <p className="diff-texto">
          <TextoDestacado texto={texto} termos={termos} />
        </p>
        {bloqueado ? (
          <p className="diff-motivo">
            <IconeAlerta tamanho={12} /> Barrado: {motivo} Fica o texto original. Se for verdade, acrescente ao perfil.
          </p>
        ) : (
          <label className="diff-alternar">
            <input type="checkbox" checked={aceito} onChange={aoAlternar} />
            {aceito ? 'usar este texto' : 'recusado: usa o original'}
          </label>
        )}
      </div>
      {originais.length > 0 && (
        <div className="diff-original">
          <span className="diff-original-rotulo">{rotuloOriginal}</span>
          {originais.map((o) => <p key={o}>{o}</p>)}
        </div>
      )}
    </div>
  )
}
