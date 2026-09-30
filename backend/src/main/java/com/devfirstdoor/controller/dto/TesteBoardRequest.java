package com.devfirstdoor.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record TesteBoardRequest(
        Ats ats,
        @NotBlank(message = "A empresa é obrigatória") String empresa
) {

    public enum Ats {
        GREENHOUSE,
        LEVER
    }
}
