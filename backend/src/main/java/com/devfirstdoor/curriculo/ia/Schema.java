package com.devfirstdoor.curriculo.ia;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Monta JSON Schema no formato que o modo {@code strict} dos Structured Outputs exige: todo objeto
 * com {@code additionalProperties: false} e todas as propriedades em {@code required} (o "opcional"
 * vira tipo anulável). Restrições de tamanho não são aceitas ali; o validador confere depois.
 */
public final class Schema {

    private Schema() {
    }

    public static Map<String, Object> objeto(Map<String, Object> propriedades) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", propriedades);
        schema.put("required", List.copyOf(propriedades.keySet()));
        schema.put("additionalProperties", false);
        return schema;
    }

    public static Map<String, Object> lista(Map<String, Object> itens) {
        return Map.of("type", "array", "items", itens);
    }

    public static Map<String, Object> texto() {
        return Map.of("type", "string");
    }

    public static Map<String, Object> textoOuNulo() {
        return Map.of("type", List.of("string", "null"));
    }

    /** Lista vazia vira um valor-sentinela: o schema não aceita enum vazio, e o validador descarta a sentinela. */
    public static Map<String, Object> umDe(List<String> valores) {
        return Map.of("type", "string", "enum", valores.isEmpty() ? List.of(SEM_OPCOES) : List.copyOf(valores));
    }

    public static final String SEM_OPCOES = "__nenhum__";

    /** Propriedades em ordem (o modelo gera os campos na ordem do schema). */
    @SafeVarargs
    public static Map<String, Object> props(Map.Entry<String, Object>... entradas) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entrada : entradas) {
            mapa.put(entrada.getKey(), entrada.getValue());
        }
        return mapa;
    }

    public static Map.Entry<String, Object> campo(String nome, Object schema) {
        return Map.entry(nome, schema);
    }
}
