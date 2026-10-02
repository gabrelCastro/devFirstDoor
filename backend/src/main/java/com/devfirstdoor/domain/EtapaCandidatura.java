package com.devfirstdoor.domain;

import java.util.EnumSet;
import java.util.Set;

/** Etapas de uma candidatura. As cinco primeiras são o quadro; as demais encerram o processo. */
public enum EtapaCandidatura {
    INTERESSE,
    CANDIDATADO,
    TESTE_TECNICO,
    ENTREVISTA,
    OFERTA,
    CONTRATADO,
    REPROVADO,
    DESISTENCIA,
    SEM_RETORNO;

    /** Etapas que só existem se a empresa respondeu: contam para a taxa de resposta. */
    private static final Set<EtapaCandidatura> RESPOSTAS =
            EnumSet.of(TESTE_TECNICO, ENTREVISTA, OFERTA, CONTRATADO, REPROVADO);

    /** Chegar aqui implica que a candidatura foi enviada. */
    private static final Set<EtapaCandidatura> ENVIADAS =
            EnumSet.of(CANDIDATADO, TESTE_TECNICO, ENTREVISTA, OFERTA, CONTRATADO, REPROVADO, SEM_RETORNO);

    public boolean isEncerrada() {
        return ordinal() >= CONTRATADO.ordinal();
    }

    public boolean isResposta() {
        return RESPOSTAS.contains(this);
    }

    public boolean impliesEnvio() {
        return ENVIADAS.contains(this);
    }

    public String rotulo() {
        return switch (this) {
            case INTERESSE -> "Interesse";
            case CANDIDATADO -> "Candidatei";
            case TESTE_TECNICO -> "Teste técnico";
            case ENTREVISTA -> "Entrevista";
            case OFERTA -> "Oferta";
            case CONTRATADO -> "Contratado";
            case REPROVADO -> "Reprovado";
            case DESISTENCIA -> "Desisti";
            case SEM_RETORNO -> "Sem retorno";
        };
    }
}
