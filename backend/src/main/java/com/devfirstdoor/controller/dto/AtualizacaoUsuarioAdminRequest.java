package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.Papel;

public record AtualizacaoUsuarioAdminRequest(
        Papel papel,
        Boolean ativo
) {
}
