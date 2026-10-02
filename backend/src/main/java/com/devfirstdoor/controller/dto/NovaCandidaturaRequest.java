package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.EtapaCandidatura;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Com {@code vagaId}, acompanha uma vaga coletada (os demais campos são ignorados). Sem ele,
 * registra uma candidatura externa: título e empresa obrigatórios.
 */
public record NovaCandidaturaRequest(
        Long vagaId,
        @Size(max = 255, message = "O título passa de 255 caracteres") String titulo,
        @Size(max = 255, message = "A empresa passa de 255 caracteres") String empresa,
        @Size(max = 255, message = "O local passa de 255 caracteres") String local,
        @Size(max = 1024, message = "O link passa de 1024 caracteres") String link,
        EtapaCandidatura etapa,
        LocalDate dataCandidatura
) {
}
