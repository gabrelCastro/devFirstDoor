package com.devfirstdoor.curriculo.ia;

import java.util.Map;

/** Porta para o modelo de linguagem; os testes usam uma implementação falsa e nunca chamam a API real. */
public interface ClienteIa {

    boolean configurado();

    /**
     * Pede uma resposta em JSON que obedece ao {@code schema} (Structured Outputs) e devolve esse JSON.
     * {@code instrucoes} vai como mensagem de sistema; {@code entrada}, como mensagem do usuário.
     */
    String gerarJson(String instrucoes, String entrada, String nomeSchema, Map<String, Object> schema);
}
