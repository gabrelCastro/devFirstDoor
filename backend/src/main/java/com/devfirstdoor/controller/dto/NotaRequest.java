package com.devfirstdoor.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotaRequest(
        @NotBlank(message = "Escreva a nota")
        @Size(max = 2000, message = "A nota passa de 2000 caracteres")
        String texto
) {
}
