package com.devfirstdoor.controller.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Substitui o próximo passo; texto e data nulos (ou texto em branco) limpam. */
public record ProximoPassoRequest(
        @Size(max = 200, message = "O próximo passo passa de 200 caracteres") String texto,
        LocalDate data
) {
}
