package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.EtapaCandidatura;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Funil pessoal. {@code taxaResposta} (0 a 1) e {@code diasMedioAteResposta} são nulos sem
 * dados suficientes. {@code semanas} traz as últimas 12 semanas (segunda a domingo), da mais antiga
 * para a atual, com quantas candidaturas foram enviadas em cada.
 */
public record ResumoCandidaturasResponse(
        int total,
        int ativas,
        int encerradas,
        Map<EtapaCandidatura, Integer> porEtapa,
        int enviadas,
        int enviadasNoMes,
        int responderam,
        Double taxaResposta,
        Double diasMedioAteResposta,
        int followUps,
        List<Semana> semanas
) {

    public record Semana(LocalDate inicio, int enviadas) {
    }
}
