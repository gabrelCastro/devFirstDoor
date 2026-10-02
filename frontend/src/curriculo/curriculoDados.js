// Regras do currículo sem React, testadas com node --test.

export const SITUACOES = [
  { id: 'CURSANDO', rotulo: 'cursando' },
  { id: 'CONCLUIDO', rotulo: 'concluído' },
  { id: 'TRANCADO', rotulo: 'trancado' },
]

export const NIVEIS_IDIOMA = ['Básico', 'Intermediário', 'Avançado', 'Fluente', 'Nativo']

/** Mínimo de caracteres da descrição da vaga (igual ao backend). */
export const DESCRICAO_MIN = 150
export const DESCRICAO_MAX = 20000

export function perfilVazio() {
  return {
    contato: { nome: '', email: '', telefone: '', cidade: '', linkedin: '', github: '', portfolio: '' },
    resumo: '',
    experiencias: [],
    projetos: [],
    formacoes: [],
    cursos: [],
    habilidades: [],
    idiomas: [],
  }
}

/** Completa campos ausentes (o backend omite nulos) para os inputs controlados não reclamarem. */
export function prepararPerfil(perfil) {
  const base = perfilVazio()
  const p = perfil ?? {}
  const contato = { ...base.contato }
  for (const chave of Object.keys(contato)) contato[chave] = p.contato?.[chave] ?? ''
  return {
    contato,
    resumo: p.resumo ?? '',
    experiencias: (p.experiencias ?? []).map((e) => ({
      ...novaExperiencia(), ...semNulos(e), bullets: (e.bullets ?? []).map(semNulos), tecnologias: e.tecnologias ?? [],
    })),
    projetos: (p.projetos ?? []).map((pr) => ({
      ...novoProjeto(), ...semNulos(pr), bullets: (pr.bullets ?? []).map(semNulos), tecnologias: pr.tecnologias ?? [],
    })),
    formacoes: (p.formacoes ?? []).map((f) => ({ ...novaFormacao(), ...semNulos(f) })),
    cursos: (p.cursos ?? []).map((c) => ({ ...novoCurso(), ...semNulos(c) })),
    habilidades: p.habilidades ?? [],
    idiomas: (p.idiomas ?? []).map((i) => ({ ...novoIdioma(), ...semNulos(i) })),
  }
}

function semNulos(objeto) {
  return Object.fromEntries(Object.entries(objeto ?? {}).map(([k, v]) => [k, v ?? '']))
}

export const novaExperiencia = () => ({ id: '', cargo: '', empresa: '', local: '', inicio: '', fim: '', bullets: [{ id: '', texto: '' }], tecnologias: [] })
export const novoProjeto = () => ({ id: '', nome: '', link: '', bullets: [{ id: '', texto: '' }], tecnologias: [] })
export const novaFormacao = () => ({ id: '', curso: '', instituicao: '', inicio: '', fim: '', situacao: 'CURSANDO' })
export const novoCurso = () => ({ id: '', nome: '', instituicao: '', ano: '' })
export const novoIdioma = () => ({ id: '', idioma: '', nivel: 'Intermediário' })

/** Separa "Java, Spring Boot;  Docker" em termos sem repetir (sem diferenciar maiúsculas). */
export function separarTermos(texto) {
  const vistos = new Set()
  const termos = []
  for (const parte of (texto ?? '').split(/[,;\n]/)) {
    const termo = parte.trim()
    const chave = termo.toLowerCase()
    if (termo && !vistos.has(chave)) {
      vistos.add(chave)
      termos.push(termo)
    }
  }
  return termos
}

/** O que falta para o currículo ficar bom, na ordem em que vale a pena resolver. */
export function pendencias(perfil) {
  const faltas = []
  if (!perfil.contato.nome.trim()) faltas.push('seu nome')
  if (!perfil.contato.email.trim()) faltas.push('um e-mail de contato')
  if (perfil.experiencias.length === 0 && perfil.projetos.length === 0) faltas.push('uma experiência ou projeto')
  if (perfil.formacoes.length === 0) faltas.push('sua formação')
  if (perfil.habilidades.length === 0) faltas.push('suas habilidades')
  const semTopicos = [...perfil.experiencias, ...perfil.projetos].some((item) => !item.bullets.some((b) => b.texto.trim()))
  if (semTopicos) faltas.push('tópicos em cada experiência e projeto')
  return faltas
}

/** Pode pedir a adaptação: tem experiência ou projeto salvo. */
export function podeAdaptar(perfil) {
  return perfil.experiencias.length > 0 || perfil.projetos.length > 0
}

function normalizar(texto) {
  return (texto ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
}

/**
 * Quebra o texto em pedaços marcando as palavras-chave da vaga (sem diferenciar maiúsculas e
 * acentos), para destacar no diff onde o tópico usa o vocabulário da vaga.
 */
export function destacar(texto, termos) {
  const alvo = normalizar(texto)
  const marcas = new Array(texto.length).fill(false)
  for (const termo of termos ?? []) {
    const t = normalizar(termo).trim()
    if (!t) continue
    let inicio = alvo.indexOf(t)
    while (inicio !== -1) {
      const antes = alvo[inicio - 1]
      const depois = alvo[inicio + t.length]
      const borda = (c) => c === undefined || !/[\p{L}\p{N}]/u.test(c)
      if (borda(antes) && borda(depois)) for (let i = inicio; i < inicio + t.length; i++) marcas[i] = true
      inicio = alvo.indexOf(t, inicio + 1)
    }
  }
  const pedacos = []
  for (let i = 0; i < texto.length; i++) {
    const ultimo = pedacos[pedacos.length - 1]
    if (ultimo && ultimo.destaque === marcas[i]) ultimo.texto += texto[i]
    else pedacos.push({ texto: texto[i], destaque: marcas[i] })
  }
  return pedacos
}

/** Liga/desliga a recusa de um tópico, sem repetir chaves. */
export function alternarRecusa(escolhas, chave) {
  const recusados = new Set(escolhas.recusados ?? [])
  if (recusados.has(chave)) recusados.delete(chave)
  else recusados.add(chave)
  return { ...escolhas, recusados: [...recusados] }
}

export function resumoDaProposta(proposta) {
  const bullets = [...proposta.experiencias, ...proposta.projetos].flatMap((bloco) => bloco.bullets)
  return {
    total: bullets.length,
    bloqueados: bullets.filter((b) => b.status === 'BLOQUEADO').length,
  }
}

/** Palavras-chave da vaga que continuam fora do currículo adaptado. */
export function palavrasFaltando(cobertura) {
  const depois = new Set(cobertura.depois)
  return cobertura.palavrasChave.filter((p) => !depois.has(p))
}
