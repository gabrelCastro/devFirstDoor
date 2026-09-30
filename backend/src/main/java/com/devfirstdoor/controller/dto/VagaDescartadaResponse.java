package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.MotivoDescarte;
import com.devfirstdoor.domain.VagaDescartada;

import java.time.LocalDateTime;

public record VagaDescartadaResponse(
        long id,
        String fonte,
        String titulo,
        String empresa,
        String local,
        String link,
        MotivoDescarte motivo,
        LocalDateTime data
) {
    public static VagaDescartadaResponse from(VagaDescartada descarte) {
        return new VagaDescartadaResponse(
                descarte.getId(),
                descarte.getFonte(),
                descarte.getTitulo(),
                descarte.getEmpresa(),
                descarte.getLocal(),
                descarte.getLink(),
                descarte.getMotivo(),
                descarte.getDataDescarte());
    }
}
