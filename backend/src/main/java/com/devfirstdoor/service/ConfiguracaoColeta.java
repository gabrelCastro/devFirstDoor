package com.devfirstdoor.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Retrato imutável usado durante uma coleta para não misturar alterações simultâneas. */
public record ConfiguracaoColeta(
        Map<String, Boolean> fontesLigadas,
        List<String> termosBuscaGupy,
        List<String> termosBuscaLinkedin,
        List<String> empresasGreenhouse,
        List<String> empresasLever,
        Duration intervaloColeta,
        int diasParaExpirar,
        long pausaLinkedinMs,
        long variacaoPausaLinkedinMs,
        boolean agendamentoPausado,
        boolean linkedinChaveMestraAtiva
) {
    public boolean fonteLigada(String fonte) {
        boolean configurada = fontesLigadas.getOrDefault(fonte, false);
        return !"LINKEDIN".equals(fonte) ? configurada : configurada && linkedinChaveMestraAtiva;
    }
}
