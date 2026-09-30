package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.StatusVaga;

public record AtualizacaoVagaAdminRequest(
        StatusVaga status,
        NivelVaga nivel,
        Boolean remoto
) {
}
