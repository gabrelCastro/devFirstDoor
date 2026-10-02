import { useEffect, useMemo, useState } from 'react'
import { IconeDownload, IconeMais } from '../icons'
import { ErroNaoAutenticado } from '../conta/sessao'
import { baixarPerfil, salvarPerfil } from './api'
import { CampoArea, CampoTermos, CampoTexto, CartaoItem, ListaTopicos } from './CamposPerfil'
import {
  NIVEIS_IDIOMA,
  SITUACOES,
  novaExperiencia,
  novaFormacao,
  novoCurso,
  novoIdioma,
  novoProjeto,
  pendencias,
  prepararPerfil,
} from './curriculoDados'

function mesAtual() {
  const agora = new Date()
  return `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, '0')}`
}

/** Edita o perfil-mestre: fatos que a IA vai usar (e só eles). */
export default function EditorPerfil({ perfilInicial, aoSalvar, aoExpirar }) {
  const [perfil, setPerfil] = useState(perfilInicial)
  const [salvo, setSalvo] = useState(() => JSON.stringify(perfilInicial))
  const [salvando, setSalvando] = useState(false)
  const [mensagem, setMensagem] = useState(null)
  const [baixando, setBaixando] = useState(null)
  const pendente = useMemo(() => JSON.stringify(perfil) !== salvo, [perfil, salvo])
  const faltas = pendencias(perfil)

  useEffect(() => {
    if (!pendente) return
    const avisar = (e) => e.preventDefault()
    window.addEventListener('beforeunload', avisar)
    return () => window.removeEventListener('beforeunload', avisar)
  }, [pendente])

  const alterar = (campo, valor) => setPerfil((p) => ({ ...p, [campo]: valor }))
  const alterarContato = (campo, valor) => setPerfil((p) => ({ ...p, contato: { ...p.contato, [campo]: valor } }))
  const alterarItem = (lista, indice, campo, valor) =>
    setPerfil((p) => ({ ...p, [lista]: p[lista].map((item, i) => (i === indice ? { ...item, [campo]: valor } : item)) }))
  const moverItem = (lista, indice, delta) =>
    setPerfil((p) => {
      const destino = indice + delta
      if (destino < 0 || destino >= p[lista].length) return p
      const nova = [...p[lista]]
      ;[nova[indice], nova[destino]] = [nova[destino], nova[indice]]
      return { ...p, [lista]: nova }
    })
  const removerItem = (lista, indice) => setPerfil((p) => ({ ...p, [lista]: p[lista].filter((_, i) => i !== indice) }))
  const adicionarItem = (lista, fabrica) => setPerfil((p) => ({ ...p, [lista]: [...p[lista], fabrica()] }))

  async function salvar(evento) {
    evento?.preventDefault()
    setSalvando(true)
    setMensagem(null)
    try {
      const resposta = prepararPerfil(await salvarPerfil(perfil))
      setPerfil(resposta)
      setSalvo(JSON.stringify(resposta))
      setMensagem({ tipo: 'ok', texto: 'Perfil salvo.' })
      aoSalvar(resposta)
    } catch (e) {
      if (e instanceof ErroNaoAutenticado) aoExpirar()
      else setMensagem({ tipo: 'erro', texto: e.message })
    } finally {
      setSalvando(false)
    }
  }

  async function baixar(formato) {
    setBaixando(formato)
    setMensagem(null)
    try {
      await baixarPerfil(formato)
    } catch (e) {
      if (e instanceof ErroNaoAutenticado) aoExpirar()
      else setMensagem({ tipo: 'erro', texto: e.message })
    } finally {
      setBaixando(null)
    }
  }

  return (
    <form className="editor-cv" onSubmit={salvar} noValidate autoComplete="off">
      {faltas.length > 0 ? (
        <div className="pendencias" role="status">
          <p className="pendencias-titulo">Para um currículo completo, falta:</p>
          <ul>{faltas.map((f) => <li key={f}>{f}</li>)}</ul>
        </div>
      ) : (
        <p className="mensagem">Perfil completo. Agora é só adaptar para cada vaga.</p>
      )}

      <section className="secao-cv" aria-labelledby="cv-contato">
        <h3 id="cv-contato">Contato</h3>
        <p className="secao-cv-ajuda">Vai no topo do currículo. Não é enviado para a IA.</p>
        <div className="grade-cv">
          <CampoTexto rotulo="Nome completo" valor={perfil.contato.nome} aoMudar={(v) => alterarContato('nome', v)} maximo={120} autoComplete="name" />
          <CampoTexto rotulo="E-mail" tipo="email" valor={perfil.contato.email} aoMudar={(v) => alterarContato('email', v)} autoComplete="email" />
          <CampoTexto rotulo="Telefone" tipo="tel" valor={perfil.contato.telefone} aoMudar={(v) => alterarContato('telefone', v)} maximo={40} autoComplete="tel" />
          <CampoTexto rotulo="Cidade" valor={perfil.contato.cidade} aoMudar={(v) => alterarContato('cidade', v)} maximo={100} placeholder="Recife, PE" />
          <CampoTexto rotulo="LinkedIn" tipo="url" valor={perfil.contato.linkedin} aoMudar={(v) => alterarContato('linkedin', v)} maximo={300} placeholder="https://linkedin.com/in/..." />
          <CampoTexto rotulo="GitHub" tipo="url" valor={perfil.contato.github} aoMudar={(v) => alterarContato('github', v)} maximo={300} placeholder="https://github.com/..." />
          <CampoTexto rotulo="Portfólio" tipo="url" valor={perfil.contato.portfolio} aoMudar={(v) => alterarContato('portfolio', v)} maximo={300} placeholder="https://" />
        </div>
      </section>

      <section className="secao-cv" aria-labelledby="cv-resumo">
        <h3 id="cv-resumo">Resumo</h3>
        <CampoArea
          rotulo="Quem é você profissionalmente"
          valor={perfil.resumo}
          aoMudar={(v) => alterar('resumo', v)}
          maximo={1500}
          linhas={3}
          dica="2 ou 3 frases. Na adaptação, a IA reescreve pensando na vaga."
        />
      </section>

      <section className="secao-cv" aria-labelledby="cv-exp">
        <h3 id="cv-exp">Experiência</h3>
        <p className="secao-cv-ajuda">Estágios, empregos, monitoria, trabalho voluntário. Escreva o que você fez de verdade: é daqui que a IA tira os fatos.</p>
        {perfil.experiencias.map((e, i) => (
          <CartaoItem
            key={e.id || `exp-${i}`}
            titulo={e.cargo || e.empresa || `Experiência ${i + 1}`}
            indice={i}
            total={perfil.experiencias.length}
            aoMover={(d) => moverItem('experiencias', i, d)}
            aoRemover={() => removerItem('experiencias', i)}
          >
            <div className="grade-cv">
              <CampoTexto rotulo="Cargo" valor={e.cargo} aoMudar={(v) => alterarItem('experiencias', i, 'cargo', v)} placeholder="Estagiário de Desenvolvimento" />
              <CampoTexto rotulo="Empresa" valor={e.empresa} aoMudar={(v) => alterarItem('experiencias', i, 'empresa', v)} />
              <CampoTexto rotulo="Local" valor={e.local} aoMudar={(v) => alterarItem('experiencias', i, 'local', v)} maximo={100} placeholder="Remoto, Recife..." />
              <CampoTexto rotulo="Início" tipo="month" valor={e.inicio} aoMudar={(v) => alterarItem('experiencias', i, 'inicio', v)} maximo={7} />
              <div className="campo-cv">
                <span className="campo-cv-rotulo">Fim</span>
                <label className="caixa-cv">
                  <input
                    type="checkbox"
                    checked={!e.fim}
                    onChange={(ev) => alterarItem('experiencias', i, 'fim', ev.target.checked ? '' : mesAtual())}
                  />
                  trabalho aqui atualmente
                </label>
                {e.fim && (
                  <input type="month" aria-label="Mês de saída" value={e.fim} onChange={(ev) => alterarItem('experiencias', i, 'fim', ev.target.value)} />
                )}
              </div>
            </div>
            <ListaTopicos rotulo="O que você fez" topicos={e.bullets} aoMudar={(v) => alterarItem('experiencias', i, 'bullets', v)} />
            <CampoTermos rotulo="Tecnologias usadas" termos={e.tecnologias} aoMudar={(v) => alterarItem('experiencias', i, 'tecnologias', v)} sugestao="Java, Spring Boot, PostgreSQL" maximo={25} />
          </CartaoItem>
        ))}
        <button type="button" className="botao" onClick={() => adicionarItem('experiencias', novaExperiencia)}>
          <IconeMais tamanho={12} /> experiência
        </button>
      </section>

      <section className="secao-cv" aria-labelledby="cv-proj">
        <h3 id="cv-proj">Projetos</h3>
        <p className="secao-cv-ajuda">Para quem está começando, projetos pesam tanto quanto experiência: faculdade, pessoais, hackathons.</p>
        {perfil.projetos.map((p, i) => (
          <CartaoItem
            key={p.id || `proj-${i}`}
            titulo={p.nome || `Projeto ${i + 1}`}
            indice={i}
            total={perfil.projetos.length}
            aoMover={(d) => moverItem('projetos', i, d)}
            aoRemover={() => removerItem('projetos', i)}
          >
            <div className="grade-cv">
              <CampoTexto rotulo="Nome" valor={p.nome} aoMudar={(v) => alterarItem('projetos', i, 'nome', v)} />
              <CampoTexto rotulo="Link" tipo="url" valor={p.link} aoMudar={(v) => alterarItem('projetos', i, 'link', v)} maximo={300} placeholder="https://github.com/..." />
            </div>
            <ListaTopicos rotulo="O que o projeto faz e o que você fez" topicos={p.bullets} aoMudar={(v) => alterarItem('projetos', i, 'bullets', v)} />
            <CampoTermos rotulo="Tecnologias" termos={p.tecnologias} aoMudar={(v) => alterarItem('projetos', i, 'tecnologias', v)} sugestao="React, Node.js" maximo={25} />
          </CartaoItem>
        ))}
        <button type="button" className="botao" onClick={() => adicionarItem('projetos', novoProjeto)}>
          <IconeMais tamanho={12} /> projeto
        </button>
      </section>

      <section className="secao-cv" aria-labelledby="cv-form">
        <h3 id="cv-form">Formação</h3>
        {perfil.formacoes.map((f, i) => (
          <CartaoItem
            key={f.id || `form-${i}`}
            titulo={f.curso || `Formação ${i + 1}`}
            indice={i}
            total={perfil.formacoes.length}
            aoMover={(d) => moverItem('formacoes', i, d)}
            aoRemover={() => removerItem('formacoes', i)}
          >
            <div className="grade-cv">
              <CampoTexto rotulo="Curso" valor={f.curso} aoMudar={(v) => alterarItem('formacoes', i, 'curso', v)} placeholder="Análise e Desenvolvimento de Sistemas" />
              <CampoTexto rotulo="Instituição" valor={f.instituicao} aoMudar={(v) => alterarItem('formacoes', i, 'instituicao', v)} />
              <CampoTexto rotulo="Início" tipo="month" valor={f.inicio} aoMudar={(v) => alterarItem('formacoes', i, 'inicio', v)} maximo={7} />
              <CampoTexto rotulo="Conclusão (ou previsão)" tipo="month" valor={f.fim} aoMudar={(v) => alterarItem('formacoes', i, 'fim', v)} maximo={7} />
              <div className="campo-cv">
                <label htmlFor={`situacao-${i}`}>Situação</label>
                <select id={`situacao-${i}`} value={f.situacao || 'CURSANDO'} onChange={(e) => alterarItem('formacoes', i, 'situacao', e.target.value)}>
                  {SITUACOES.map((s) => <option key={s.id} value={s.id}>{s.rotulo}</option>)}
                </select>
              </div>
            </div>
          </CartaoItem>
        ))}
        <button type="button" className="botao" onClick={() => adicionarItem('formacoes', novaFormacao)}>
          <IconeMais tamanho={12} /> formação
        </button>
      </section>

      <section className="secao-cv" aria-labelledby="cv-cursos">
        <h3 id="cv-cursos">Cursos e certificações</h3>
        {perfil.cursos.map((c, i) => (
          <div key={c.id || `curso-${i}`} className="linha-cv">
            <CampoTexto rotulo="Curso" valor={c.nome} aoMudar={(v) => alterarItem('cursos', i, 'nome', v)} />
            <CampoTexto rotulo="Instituição" valor={c.instituicao} aoMudar={(v) => alterarItem('cursos', i, 'instituicao', v)} />
            <CampoTexto rotulo="Ano" valor={c.ano} aoMudar={(v) => alterarItem('cursos', i, 'ano', v.replace(/\D/g, ''))} maximo={4} inputMode="numeric" />
            <button type="button" className="botao-limpar" onClick={() => removerItem('cursos', i)}>remover</button>
          </div>
        ))}
        <button type="button" className="botao" onClick={() => adicionarItem('cursos', novoCurso)}>
          <IconeMais tamanho={12} /> curso
        </button>
      </section>

      <section className="secao-cv" aria-labelledby="cv-hab">
        <h3 id="cv-hab">Habilidades</h3>
        <CampoTermos rotulo="Tecnologias, ferramentas e competências" termos={perfil.habilidades} aoMudar={(v) => alterar('habilidades', v)} sugestao="Git, SQL, Docker, Scrum" />
      </section>

      <section className="secao-cv" aria-labelledby="cv-idiomas">
        <h3 id="cv-idiomas">Idiomas</h3>
        {perfil.idiomas.map((idioma, i) => (
          <div key={idioma.id || `idioma-${i}`} className="linha-cv">
            <CampoTexto rotulo="Idioma" valor={idioma.idioma} aoMudar={(v) => alterarItem('idiomas', i, 'idioma', v)} maximo={40} />
            <div className="campo-cv">
              <label htmlFor={`nivel-${i}`}>Nível</label>
              <select id={`nivel-${i}`} value={idioma.nivel || 'Intermediário'} onChange={(e) => alterarItem('idiomas', i, 'nivel', e.target.value)}>
                {NIVEIS_IDIOMA.map((n) => <option key={n}>{n}</option>)}
              </select>
            </div>
            <button type="button" className="botao-limpar" onClick={() => removerItem('idiomas', i)}>remover</button>
          </div>
        ))}
        <button type="button" className="botao" onClick={() => adicionarItem('idiomas', novoIdioma)}>
          <IconeMais tamanho={12} /> idioma
        </button>
      </section>

      <div className="barra-salvar" data-pendente={pendente || undefined}>
        <p role="status" className={mensagem?.tipo === 'erro' ? 'barra-salvar-erro' : undefined}>
          {mensagem?.texto ?? (pendente ? 'Há alterações não salvas.' : 'Tudo salvo.')}
        </p>
        <div className="barra-salvar-acoes">
          <button type="button" className="botao" onClick={() => baixar('pdf')} disabled={pendente || baixando != null} aria-busy={baixando === 'pdf'} title={pendente ? 'Salve antes de baixar' : undefined}>
            <IconeDownload tamanho={12} /> pdf
          </button>
          <button type="button" className="botao" onClick={() => baixar('docx')} disabled={pendente || baixando != null} aria-busy={baixando === 'docx'} title={pendente ? 'Salve antes de baixar' : undefined}>
            <IconeDownload tamanho={12} /> docx
          </button>
          <button type="submit" className="botao botao-primario" disabled={salvando || !pendente} aria-busy={salvando}>
            {salvando ? 'salvando...' : 'salvar perfil'}
          </button>
        </div>
      </div>
    </form>
  )
}
