package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.MotivoDescarte;
import com.devfirstdoor.domain.NivelVaga;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record MetricasResponse(
        Map<String, Long> vagasAtivasPorFonte,
        Map<NivelVaga, Long> vagasAtivasPorNivel,
        Map<Modalidade, Long> vagasAtivasPorModalidade,
        List<VagasNovasDia> vagasNovasUltimos30Dias,
        double tempoMedioNoArHoras,
        Map<String, Long> vagasExclusivasPorFonte,
        Map<MotivoDescarte, Long> descartesPorMotivoUltimos7Dias
) {

    public enum Modalidade {
        REMOTA,
        NAO_REMOTA
    }

    public record VagasNovasDia(LocalDate data, String fonte, long total) {
    }
}
