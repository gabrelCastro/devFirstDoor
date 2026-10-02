package com.devfirstdoor.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
        @NotNull(message = "Informe o usuário")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,40}",
                message = "O usuário deve ter de 3 a 40 caracteres: letras, números, ponto, hífen ou sublinhado")
        String usuario,

        @NotNull(message = "Informe a senha")
        @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres")
        String senha
) {
}
