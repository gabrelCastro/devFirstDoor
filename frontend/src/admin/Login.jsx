import { ErroSemPermissao, chamarAdmin } from './api'
import { entrar as entrarNaApi } from '../conta/sessao'
import FormularioEntrar from '../conta/FormularioEntrar'
import '../conta/conta.css'

/** O login vale para qualquer conta; aqui só passa quem tem o papel ADMIN. */
async function entrarComoAdmin(usuario, senha) {
  await entrarNaApi(usuario, senha)
  try {
    return await chamarAdmin('/me')
  } catch (e) {
    if (e instanceof ErroSemPermissao) throw new Error('Esta conta não tem acesso à área administrativa.')
    throw e
  }
}

export default function Login({ aoEntrar, aviso }) {
  return (
    <section className="admin-login" aria-labelledby="admin-login-titulo">
      <header className="admin-login-cabecalho">
        <p className="admin-login-kicker">$ acesso restrito</p>
        <h2 id="admin-login-titulo" className="admin-login-titulo">Área administrativa</h2>
        <p className="admin-login-subtitulo">Entre com uma conta de administrador.</p>
      </header>
      {aviso && (
        <p className="formulario-conta-erro" role="alert">
          {aviso}
        </p>
      )}
      <FormularioEntrar
        permitirCadastro={false}
        entrar={entrarComoAdmin}
        aoConcluir={(dados) => aoEntrar(dados.usuario)}
      />
    </section>
  )
}
