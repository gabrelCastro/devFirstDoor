package com.devfirstdoor.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrocaSenhaRequest(
        @NotNull(message = "Informe a senha atual")
        String senhaAtual,

        @NotNull(message = "Informe a nova senha")
        @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres")
        String novaSenha
) {
}
