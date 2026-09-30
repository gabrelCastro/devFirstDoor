package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VagaResponse(
        Long id,
        String titulo,
        String empresa,
        String local,
        NivelVaga nivel,
        String link,
        String fonte,
        LocalDate dataPublicacao,
        LocalDateTime dataColeta,
        boolean internacional,
        boolean remoto
) {

    public static VagaResponse from(Vaga vaga) {
        return new VagaResponse(
                vaga.getId(),
                vaga.getTitulo(),
                vaga.getEmpresa(),
                vaga.getLocal(),
                vaga.getNivel(),
                vaga.getLink(),
                vaga.getFonte(),
                vaga.getDataPublicacao(),
                vaga.getDataColeta(),
                vaga.isInternacional(),
                vaga.isRemoto()
        );
    }
}
