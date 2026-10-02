import { forwardRef, useId, useState } from 'react'

/**
 * Campo de formulário com rótulo, dica e erro ligados ao input por aria-describedby; o erro
 * também marca aria-invalid. {@code direita} é um elemento dentro da caixa (ex.: mostrar senha).
 */
export const Campo = forwardRef(function Campo({ rotulo, dica, erro, aviso, direita, ...input }, ref) {
  const id = useId()
  // Com erro, a dica sai: o erro já diz a regra que faltou, e repetir só polui.
  const mostrarDica = dica && !erro
  const idDica = mostrarDica ? `${id}-dica` : null
  const idErro = erro ? `${id}-erro` : null
  const idAviso = aviso ? `${id}-aviso` : null
  const descritores = [idErro, idAviso, idDica].filter(Boolean).join(' ') || undefined

  return (
    <div className="campo-conta" data-invalido={erro ? true : undefined}>
      <label htmlFor={id}>{rotulo}</label>
      <div className="campo-conta-caixa">
        <input ref={ref} id={id} aria-invalid={erro ? true : undefined} aria-describedby={descritores} {...input} />
        {direita}
      </div>
      {erro && (
        <p id={idErro} className="campo-conta-erro">
          {erro}
        </p>
      )}
      {aviso && !erro && (
        <p id={idAviso} className="campo-conta-aviso">
          {aviso}
        </p>
      )}
      {mostrarDica && (
        <p id={idDica} className="campo-conta-dica">
          {dica}
        </p>
      )}
    </div>
  )
})

/** Senha com botão de mostrar/ocultar e aviso de Caps Lock ligado. */
export const CampoSenha = forwardRef(function CampoSenha({ aviso, ...props }, ref) {
  const [visivel, setVisivel] = useState(false)
  const [capsLock, setCapsLock] = useState(false)

  function lerCapsLock(evento) {
    if (typeof evento.getModifierState === 'function') setCapsLock(evento.getModifierState('CapsLock'))
  }

  return (
    <Campo
      ref={ref}
      {...props}
      type={visivel ? 'text' : 'password'}
      autoCapitalize="none"
      spellCheck={false}
      onKeyDown={lerCapsLock}
      onKeyUp={lerCapsLock}
      onBlur={(evento) => {
        setCapsLock(false)
        props.onBlur?.(evento)
      }}
      aviso={aviso ?? (capsLock ? 'Caps Lock está ligado.' : null)}
      direita={
        <button
          type="button"
          className="campo-conta-revelar"
          onClick={() => setVisivel((valor) => !valor)}
          aria-pressed={visivel}
          aria-label={visivel ? 'Ocultar senha' : 'Mostrar senha'}
        >
          {visivel ? 'ocultar' : 'mostrar'}
        </button>
      }
    />
  )
})
