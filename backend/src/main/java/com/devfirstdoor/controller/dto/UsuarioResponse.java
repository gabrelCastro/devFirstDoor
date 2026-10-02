package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.Papel;
import com.devfirstdoor.domain.Usuario;

import java.time.LocalDateTime;

/** Nunca inclui o hash da senha. */
public record UsuarioResponse(
        Long id,
        String usuario,
        Papel papel,
        boolean ativo,
        LocalDateTime criadoEm
) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getLogin(), usuario.getPapel(),
                usuario.isAtivo(), usuario.getCriadoEm());
    }
}
