package com.devfirstdoor.controller.dto;

import com.devfirstdoor.curriculo.Escolhas;
import com.devfirstdoor.curriculo.Proposta;
import com.devfirstdoor.curriculo.VagaAnalisada;

import java.time.LocalDateTime;

public record VersaoCurriculoResponse(
        Long id,
        String titulo,
        Long candidaturaId,
        LocalDateTime criadaEm,
        LocalDateTime atualizadaEm,
        VagaAnalisada vaga,
        Proposta proposta,
        Escolhas escolhas
) {
}
