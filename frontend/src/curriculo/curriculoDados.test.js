import assert from 'node:assert/strict'
import test from 'node:test'
import {
  alternarRecusa,
  destacar,
  palavrasFaltando,
  pendencias,
  podeAdaptar,
  prepararPerfil,
  resumoDaProposta,
  separarTermos,
} from './curriculoDados.js'

test('prepararPerfil completa nulos e listas ausentes', () => {
  const p = prepararPerfil({ contato: { nome: 'Maria' }, experiencias: [{ id: 'e-1', cargo: 'Dev', empresa: 'X', local: null, bullets: [{ id: 'b', texto: 't' }] }] })
  assert.equal(p.contato.email, '')
  assert.equal(p.experiencias[0].local, '')
  assert.deepEqual(p.experiencias[0].tecnologias, [])
  assert.deepEqual(p.projetos, [])
  assert.equal(p.resumo, '')
})

test('separarTermos divide por vírgula e remove repetidos', () => {
  assert.deepEqual(separarTermos('Java, java; Spring Boot\n Docker,, '), ['Java', 'Spring Boot', 'Docker'])
})

test('pendencias aponta o que falta e podeAdaptar exige experiência ou projeto', () => {
  const vazio = prepararPerfil({})
  assert.deepEqual(pendencias(vazio), ['seu nome', 'um e-mail de contato', 'uma experiência ou projeto', 'sua formação', 'suas habilidades'])
  assert.equal(podeAdaptar(vazio), false)
  const comProjeto = prepararPerfil({ projetos: [{ nome: 'Agenda', bullets: [{ texto: '' }] }] })
  assert.equal(podeAdaptar(comProjeto), true)
  assert.ok(pendencias(comProjeto).includes('tópicos em cada experiência e projeto'))
})

test('destacar marca termos sem diferenciar acentos e respeitando bordas', () => {
  const pedacos = destacar('Desenvolvi APIs REST em Java e JavaScript', ['java', 'APIs REST'])
  const marcados = pedacos.filter((p) => p.destaque).map((p) => p.texto)
  assert.deepEqual(marcados, ['APIs REST', 'Java'])
  assert.equal(pedacos.map((p) => p.texto).join(''), 'Desenvolvi APIs REST em Java e JavaScript')
  assert.deepEqual(destacar('Integração contínua', ['integracao']).filter((p) => p.destaque).map((p) => p.texto), ['Integração'])
})

test('alternarRecusa liga e desliga', () => {
  const um = alternarRecusa({ usarResumo: true, recusados: [] }, 'e:0')
  assert.deepEqual(um.recusados, ['e:0'])
  assert.deepEqual(alternarRecusa(um, 'e:0').recusados, [])
})

test('resumo da proposta e palavras faltando', () => {
  const proposta = {
    experiencias: [{ bullets: [{ status: 'OK' }, { status: 'BLOQUEADO' }] }],
    projetos: [{ bullets: [{ status: 'OK' }] }],
  }
  assert.deepEqual(resumoDaProposta(proposta), { total: 3, bloqueados: 1 })
  assert.deepEqual(palavrasFaltando({ palavrasChave: ['Java', 'Docker', 'K8s'], depois: ['Java'] }), ['Docker', 'K8s'])
})
