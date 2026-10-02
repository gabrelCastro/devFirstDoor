import { useRef, useState } from 'react'
import { Campo, CampoSenha } from './Campos'
import { USUARIO_MAX, USUARIO_MIN, SENHA_MIN, validarCadastro, validarEntrada } from './validacao'

const VAZIO = { usuario: '', senha: '', confirmacao: '' }
const ORDEM_CAMPOS = ['usuario', 'senha', 'confirmacao']

/**
 * Formulário de entrar/criar conta, usado no modal do site e na página do admin.
 * {@code permitirCadastro} desliga o modo "criar conta" (o admin não cadastra ninguém por ali).
 */
export default function FormularioEntrar({
  modoInicial = 'entrar',
  permitirCadastro = true,
  entrar,
  cadastrar,
  aoConcluir,
  aoMudarModo,
  textoBotao,
}) {
  const [modo, setModo] = useState(modoInicial)
  const [valores, setValores] = useState(VAZIO)
  const [erros, setErros] = useState({})
  // Depois da primeira tentativa de envio, os erros acompanham a digitação.
  const [tentou, setTentou] = useState(false)
  const [erroGeral, setErroGeral] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const refUsuario = useRef(null)
  const refSenha = useRef(null)
  const refConfirmacao = useRef(null)
  const criando = modo === 'criar'
  const validar = criando ? validarCadastro : validarEntrada

  function alterar(campo, valor) {
    const novos = { ...valores, [campo]: valor }
    setValores(novos)
    setErroGeral(null)
    if (tentou) setErros(validar(novos))
  }

  function trocarModo(novo) {
    setModo(novo)
    setValores((atual) => ({ ...VAZIO, usuario: atual.usuario }))
    setErros({})
    setTentou(false)
    setErroGeral(null)
    aoMudarModo?.(novo)
  }

  async function enviar(evento) {
    evento.preventDefault()
    const encontrados = validar(valores)
    setTentou(true)
    setErros(encontrados)
    const primeiro = ORDEM_CAMPOS.find((campo) => encontrados[campo])
    if (primeiro) {
      const refs = { usuario: refUsuario, senha: refSenha, confirmacao: refConfirmacao }
      refs[primeiro].current?.focus()
      return
    }
    setEnviando(true)
    setErroGeral(null)
    try {
      const login = valores.usuario.trim()
      const conta = criando ? await cadastrar(login, valores.senha) : await entrar(login, valores.senha)
      aoConcluir(conta)
    } catch (e) {
      setErroGeral(e.message)
      setEnviando(false)
      // Senha errada: limpa e devolve o foco para tentar de novo sem redigitar o usuário.
      if (!criando) {
        setValores((atual) => ({ ...atual, senha: '' }))
        refSenha.current?.focus()
      }
    }
  }

  return (
    <form className="formulario-conta" onSubmit={enviar} noValidate>
      <Campo
        ref={refUsuario}
        rotulo="Usuário"
        type="text"
        name="username"
        autoComplete="username"
        autoCapitalize="none"
        spellCheck={false}
        maxLength={USUARIO_MAX}
        value={valores.usuario}
        onChange={(e) => alterar('usuario', e.target.value)}
        erro={erros.usuario}
        dica={criando ? `De ${USUARIO_MIN} a ${USUARIO_MAX} caracteres: letras, números, ponto, hífen ou sublinhado.` : null}
      />
      <CampoSenha
        ref={refSenha}
        rotulo="Senha"
        name={criando ? 'new-password' : 'password'}
        autoComplete={criando ? 'new-password' : 'current-password'}
        value={valores.senha}
        onChange={(e) => alterar('senha', e.target.value)}
        erro={erros.senha}
        dica={criando ? `Pelo menos ${SENHA_MIN} caracteres.` : null}
      />
      {criando && (
        <CampoSenha
          ref={refConfirmacao}
          rotulo="Repita a senha"
          name="confirm-password"
          autoComplete="new-password"
          value={valores.confirmacao}
          onChange={(e) => alterar('confirmacao', e.target.value)}
          erro={erros.confirmacao}
        />
      )}

      {erroGeral && (
        <p className="formulario-conta-erro" role="alert">
          {erroGeral}
        </p>
      )}

      <button type="submit" className="botao botao-primario botao-grande" disabled={enviando} aria-busy={enviando}>
        {enviando ? (criando ? 'criando conta...' : 'entrando...') : criando ? 'Criar conta' : (textoBotao ?? 'Entrar')}
      </button>

      {permitirCadastro && (
        <p className="formulario-conta-troca">
          {criando ? 'Já tem conta?' : 'Ainda não tem conta?'}{' '}
          <button type="button" onClick={() => trocarModo(criando ? 'entrar' : 'criar')}>
            {criando ? 'Entrar' : 'Criar conta grátis'}
          </button>
        </p>
      )}
    </form>
  )
}
