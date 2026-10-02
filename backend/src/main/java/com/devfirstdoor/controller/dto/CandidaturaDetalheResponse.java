package com.devfirstdoor.controller.dto;

import java.util.List;

/** A candidatura com a linha do tempo em ordem cronológica. */
public record CandidaturaDetalheResponse(
        CandidaturaResponse candidatura,
        List<EventoCandidaturaResponse> eventos
) {
}
