package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.StatusVaga;
import com.devfirstdoor.domain.Vaga;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VagaAdminResponse(
        Long id,
        String titulo,
        String empresa,
        String local,
        NivelVaga nivel,
        boolean nivelManual,
        boolean remoto,
        boolean remotoManual,
        boolean internacional,
        StatusVaga status,
        String link,
        String fonte,
        LocalDate dataPublicacao,
        LocalDateTime dataColeta,
        LocalDateTime dataUltimaVisita
) {

    public static VagaAdminResponse from(Vaga vaga) {
        return new VagaAdminResponse(
                vaga.getId(), vaga.getTitulo(), vaga.getEmpresa(), vaga.getLocal(),
                vaga.getNivel(), vaga.isNivelManual(), vaga.isRemoto(), vaga.isRemotoManual(),
                vaga.isInternacional(), vaga.getStatus(), vaga.getLink(), vaga.getFonte(),
                vaga.getDataPublicacao(), vaga.getDataColeta(), vaga.getDataUltimaVisita());
    }
}
