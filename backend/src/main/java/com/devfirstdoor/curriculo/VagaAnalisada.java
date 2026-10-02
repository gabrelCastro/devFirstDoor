package com.devfirstdoor.curriculo;

import java.util.List;

/**
 * A descrição da vaga já organizada pela IA. Os ids dos requisitos ({@code r1}, {@code d1}...) são
 * atribuídos pelo backend depois da resposta. {@code palavrasChaveAts} guarda a grafia exata da
 * vaga, que é a que o ATS procura.
 */
public record VagaAnalisada(
        String cargo,
        String empresa,
        String nivel,
        List<Requisito> obrigatorios,
        List<Requisito> desejaveis,
        List<String> responsabilidades,
        List<String> palavrasChaveAts
) {

    public record Requisito(String id, String texto, List<String> termos) {
    }

    public List<Requisito> requisitos() {
        return java.util.stream.Stream.concat(obrigatorios.stream(), desejaveis.stream()).toList();
    }
}
