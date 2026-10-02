// Regras dos formulários de conta. Espelham as do backend (CadastroRequest/TrocaSenhaRequest),
// para o erro aparecer no campo antes de ir ao servidor.

export const USUARIO_MIN = 3
export const USUARIO_MAX = 40
export const SENHA_MIN = 8
export const SENHA_MAX_BYTES = 72

const USUARIO_VALIDO = /^[A-Za-z0-9._-]+$/

function bytes(texto) {
  return new TextEncoder().encode(texto).length
}

export function erroUsuario(usuario) {
  const valor = usuario.trim()
  if (!valor) return 'Informe o usuário.'
  if (valor.length < USUARIO_MIN || valor.length > USUARIO_MAX) {
    return `Use de ${USUARIO_MIN} a ${USUARIO_MAX} caracteres.`
  }
  if (!USUARIO_VALIDO.test(valor)) return 'Use só letras, números, ponto, hífen ou sublinhado.'
  return null
}

export function erroSenhaNova(senha) {
  if (!senha) return 'Crie uma senha.'
  if (senha.length < SENHA_MIN) return `A senha precisa de pelo menos ${SENHA_MIN} caracteres.`
  if (bytes(senha) > SENHA_MAX_BYTES) return 'A senha está longa demais.'
  return null
}

export function erroConfirmacao(senha, confirmacao) {
  if (!confirmacao) return 'Repita a senha.'
  if (senha !== confirmacao) return 'As senhas não conferem.'
  return null
}

/** Erros por campo; objeto vazio quando está tudo certo. */
export function validarEntrada({ usuario, senha }) {
  const erros = {}
  if (!usuario.trim()) erros.usuario = 'Informe o usuário.'
  if (!senha) erros.senha = 'Informe a senha.'
  return erros
}

export function validarCadastro({ usuario, senha, confirmacao }) {
  const erros = {}
  const usuarioErro = erroUsuario(usuario)
  const senhaErro = erroSenhaNova(senha)
  const confirmacaoErro = erroConfirmacao(senha, confirmacao)
  if (usuarioErro) erros.usuario = usuarioErro
  if (senhaErro) erros.senha = senhaErro
  if (confirmacaoErro) erros.confirmacao = confirmacaoErro
  return erros
}

export function validarTrocaSenha({ atual, nova, confirmacao }) {
  const erros = {}
  if (!atual) erros.atual = 'Informe a senha atual.'
  const novaErro = erroSenhaNova(nova)
  if (novaErro) erros.nova = novaErro
  else if (nova === atual) erros.nova = 'A nova senha precisa ser diferente da atual.'
  const confirmacaoErro = erroConfirmacao(nova, confirmacao)
  if (confirmacaoErro) erros.confirmacao = confirmacaoErro
  return erros
}
