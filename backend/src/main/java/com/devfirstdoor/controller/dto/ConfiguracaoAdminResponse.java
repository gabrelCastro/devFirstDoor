package com.devfirstdoor.controller.dto;

import java.util.List;
import java.util.Map;

public record ConfiguracaoAdminResponse(
        Map<String, Boolean> fontesLigadas,
        List<String> termosBuscaGupy,
        List<String> termosBuscaLinkedin,
        List<String> empresasGreenhouse,
        List<String> empresasLever,
        long intervaloColetaMinutos,
        int diasParaExpirar,
        long pausaLinkedinMs,
        long variacaoPausaLinkedinMs,
        boolean agendamentoPausado,
        boolean linkedinChaveMestraAtiva,
        boolean notificacoesTelegramLigadas,
        boolean telegramTokenPreenchido,
        String telegramChatId
) {
}
