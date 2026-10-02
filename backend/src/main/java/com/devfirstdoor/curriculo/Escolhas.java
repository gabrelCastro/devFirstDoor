package com.devfirstdoor.curriculo;

import java.util.List;

/** O que a pessoa aceitou da proposta: usar o resumo novo e quais bullets recusou (pela chave). */
public record Escolhas(boolean usarResumo, List<String> recusados) {

    public static Escolhas aceitarTudo() {
        return new Escolhas(true, List.of());
    }

    public List<String> recusados() {
        return recusados == null ? List.of() : recusados;
    }
}
