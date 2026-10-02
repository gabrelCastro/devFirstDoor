import assert from 'node:assert/strict'
import test from 'node:test'
import { erroUsuario, validarCadastro, validarEntrada, validarTrocaSenha } from './validacao.js'

test('usuário segue as regras do backend', () => {
  assert.equal(erroUsuario(''), 'Informe o usuário.')
  assert.equal(erroUsuario('ab'), 'Use de 3 a 40 caracteres.')
  assert.equal(erroUsuario('a'.repeat(41)), 'Use de 3 a 40 caracteres.')
  assert.equal(erroUsuario('joão'), 'Use só letras, números, ponto, hífen ou sublinhado.')
  assert.equal(erroUsuario('jo ao'), 'Use só letras, números, ponto, hífen ou sublinhado.')
  assert.equal(erroUsuario('maria.silva_2-x'), null)
})

test('entrar só exige os dois campos preenchidos', () => {
  assert.deepEqual(validarEntrada({ usuario: ' ', senha: '' }), {
    usuario: 'Informe o usuário.',
    senha: 'Informe a senha.',
  })
  assert.deepEqual(validarEntrada({ usuario: 'x', senha: 'y' }), {})
})

test('cadastro valida tamanho da senha, bytes e confirmação', () => {
  assert.deepEqual(validarCadastro({ usuario: 'maria', senha: 'senha-forte', confirmacao: 'senha-forte' }), {})
  assert.equal(validarCadastro({ usuario: 'maria', senha: 'curta', confirmacao: 'curta' }).senha,
    'A senha precisa de pelo menos 8 caracteres.')
  assert.equal(validarCadastro({ usuario: 'maria', senha: 'é'.repeat(40), confirmacao: 'é'.repeat(40) }).senha,
    'A senha está longa demais.')
  assert.equal(validarCadastro({ usuario: 'maria', senha: 'senha-forte', confirmacao: 'outra' }).confirmacao,
    'As senhas não conferem.')
})

test('troca de senha exige senha nova diferente da atual', () => {
  assert.equal(validarTrocaSenha({ atual: 'senha-forte', nova: 'senha-forte', confirmacao: 'senha-forte' }).nova,
    'A nova senha precisa ser diferente da atual.')
  assert.deepEqual(validarTrocaSenha({ atual: 'senha-velha', nova: 'senha-nova!', confirmacao: 'senha-nova!' }), {})
})
