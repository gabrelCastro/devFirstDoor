package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.EtapaCandidatura;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Campos nulos ficam como estão. Título, empresa, local e link só valem para externas.
 * {@code limparDataCandidatura} apaga a data de envio (nulo não serve: significaria "não mudar").
 */
public record AtualizacaoCandidaturaRequest(
        EtapaCandidatura etapa,
        LocalDate dataCandidatura,
        Boolean limparDataCandidatura,
        @Size(max = 255, message = "O título passa de 255 caracteres") String titulo,
        @Size(max = 255, message = "A empresa passa de 255 caracteres") String empresa,
        @Size(max = 255, message = "O local passa de 255 caracteres") String local,
        @Size(max = 1024, message = "O link passa de 1024 caracteres") String link
) {
}
