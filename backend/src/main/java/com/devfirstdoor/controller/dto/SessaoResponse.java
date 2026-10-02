package com.devfirstdoor.controller.dto;

import java.time.LocalDateTime;

/** O token só aparece aqui, uma vez: o banco guarda apenas o hash dele. */
public record SessaoResponse(
        String token,
        LocalDateTime expiraEm,
        UsuarioResponse usuario
) {
}
