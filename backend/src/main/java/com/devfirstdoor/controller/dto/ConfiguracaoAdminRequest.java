package com.devfirstdoor.controller.dto;

import java.util.List;
import java.util.Map;

/** Configuração editável pelo painel; o PUT substitui o conjunto inteiro. */
public record ConfiguracaoAdminRequest(
        Map<String, Boolean> fontesLigadas,
        List<String> termosBuscaGupy,
        List<String> termosBuscaLinkedin,
        List<String> empresasGreenhouse,
        List<String> empresasLever,
        Long intervaloColetaMinutos,
        Integer diasParaExpirar,
        Long pausaLinkedinMs,
        Long variacaoPausaLinkedinMs,
        Boolean agendamentoPausado
) {
}
