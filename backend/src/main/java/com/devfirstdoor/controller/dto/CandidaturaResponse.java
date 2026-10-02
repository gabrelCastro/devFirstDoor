package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.Candidatura;
import com.devfirstdoor.domain.EtapaCandidatura;
import com.devfirstdoor.domain.StatusVaga;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** {@code vagaStatus} é nulo nas externas; EXPIRADA/OCULTA indicam que a vaga saiu do ar na fonte. */
public record CandidaturaResponse(
        Long id,
        Long vagaId,
        StatusVaga vagaStatus,
        boolean externa,
        String titulo,
        String empresa,
        String local,
        String link,
        String fonte,
        EtapaCandidatura etapa,
        LocalDateTime etapaDesde,
        LocalDate dataCandidatura,
        String proximoPasso,
        LocalDate dataProximoPasso,
        LocalDateTime criadaEm,
        LocalDateTime atualizadaEm
) {

    public static CandidaturaResponse from(Candidatura c) {
        return new CandidaturaResponse(
                c.getId(),
                c.getVaga() == null ? null : c.getVaga().getId(),
                c.getVaga() == null ? null : c.getVaga().getStatus(),
                c.isExterna(),
                c.getTitulo(), c.getEmpresa(), c.getLocal(), c.getLink(), c.getFonte(),
                c.getEtapa(), c.getEtapaDesde(), c.getDataCandidatura(),
                c.getProximoPasso(), c.getDataProximoPasso(),
                c.getCriadaEm(), c.getAtualizadaEm());
    }
}
