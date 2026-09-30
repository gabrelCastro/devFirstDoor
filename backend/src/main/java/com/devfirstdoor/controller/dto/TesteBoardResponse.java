package com.devfirstdoor.controller.dto;

import java.util.List;

public record TesteBoardResponse(
        TesteBoardRequest.Ats ats,
        String empresa,
        boolean existe,
        int totalVagas,
        int totalAprovadas,
        List<ExemploAprovado> exemplosAprovados,
        List<ExemploReprovado> exemplosReprovados
) {

    public record ExemploAprovado(String titulo, String link) {
    }

    public record ExemploReprovado(String titulo, String link, MotivoReprovacao motivo) {
    }

    public enum MotivoReprovacao {
        DADOS_INCOMPLETOS,
        NIVEL,
        FORA_DE_TECNOLOGIA,
        NAO_JAVA,
        NAO_REMOTA
    }
}
