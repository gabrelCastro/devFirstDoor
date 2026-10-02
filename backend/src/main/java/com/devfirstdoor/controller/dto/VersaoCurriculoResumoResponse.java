package com.devfirstdoor.controller.dto;

import java.time.LocalDateTime;

/** Item da lista de versões: cobertura de palavras-chave antes/depois para mostrar o ganho. */
public record VersaoCurriculoResumoResponse(
        Long id,
        String titulo,
        Long candidaturaId,
        LocalDateTime criadaEm,
        int palavrasChave,
        int coberturaAntes,
        int coberturaDepois
) {
}
