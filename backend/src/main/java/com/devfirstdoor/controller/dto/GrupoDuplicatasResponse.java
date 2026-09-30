package com.devfirstdoor.controller.dto;

import java.util.List;

public record GrupoDuplicatasResponse(
        String tituloNormalizado,
        String empresaNormalizada,
        List<VagaAdminResponse> vagas
) {
}
