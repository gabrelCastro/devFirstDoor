import { useState } from 'react'
import Dialogo from './Dialogo'
import FormularioEntrar from './FormularioEntrar'
import './conta.css'

const TEXTOS = {
  entrar: { titulo: 'Entrar', subtitulo: 'Acesse seu quadro de candidaturas.' },
  criar: {
    titulo: 'Criar conta',
    subtitulo: 'Só usuário e senha. As candidaturas que você acompanhar ficam visíveis só para você.',
  },
}

export default function ModalEntrar({ motivo, abaInicial = 'entrar', entrar, cadastrar, aoConcluir, aoFechar }) {
  const [modo, setModo] = useState(abaInicial)

  return (
    <Dialogo titulo={TEXTOS[modo].titulo} subtitulo={TEXTOS[modo].subtitulo} aoFechar={aoFechar} className="dialogo-entrar">
      {motivo && <p className="dialogo-motivo">{motivo}</p>}
      <FormularioEntrar
        modoInicial={abaInicial}
        entrar={entrar}
        cadastrar={cadastrar}
        aoConcluir={aoConcluir}
        aoMudarModo={setModo}
      />
    </Dialogo>
  )
}
