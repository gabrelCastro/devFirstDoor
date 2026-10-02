package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.EtapaCandidatura;
import com.devfirstdoor.domain.EventoCandidatura;
import com.devfirstdoor.domain.TipoEventoCandidatura;

import java.time.LocalDateTime;

public record EventoCandidaturaResponse(
        Long id,
        TipoEventoCandidatura tipo,
        EtapaCandidatura etapaAnterior,
        EtapaCandidatura etapaNova,
        String texto,
        LocalDateTime data
) {

    public static EventoCandidaturaResponse from(EventoCandidatura e) {
        return new EventoCandidaturaResponse(e.getId(), e.getTipo(), e.getEtapaAnterior(), e.getEtapaNova(),
                e.getTexto(), e.getData());
    }
}
