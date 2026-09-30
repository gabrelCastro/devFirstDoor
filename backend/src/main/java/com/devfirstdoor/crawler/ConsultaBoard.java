package com.devfirstdoor.crawler;

import java.util.List;

/** Distingue um board existente e vazio de um identificador que não existe no ATS. */
public record ConsultaBoard<T>(boolean existe, List<T> vagas) {

    public ConsultaBoard {
        vagas = vagas == null ? List.of() : List.copyOf(vagas);
    }

    public static <T> ConsultaBoard<T> existente(List<T> vagas) {
        return new ConsultaBoard<>(true, vagas);
    }

    public static <T> ConsultaBoard<T> inexistente() {
        return new ConsultaBoard<>(false, List.of());
    }
}
