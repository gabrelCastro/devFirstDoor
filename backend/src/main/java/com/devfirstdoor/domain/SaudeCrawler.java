package com.devfirstdoor.domain;

import java.util.List;
import java.util.Optional;

/**
 * Resumo de como uma fonte está indo, calculado a partir do histórico de execuções.
 */
public enum SaudeCrawler {
    OK,
    /** Várias coletas seguidas "funcionaram" sem achar nada: sinal de HTML/API que mudou. */
    ALERTA,
    FALHA,
    SEM_DADOS,
    DESLIGADA;

    /** Quantas execuções com sucesso seguidas sem nenhuma vaga viram {@link #ALERTA}. */
    public static final int SUCESSOS_VAZIOS_PARA_ALERTA = 3;

    /**
     * @param ultimaExecucao    a execução mais recente da fonte, com sucesso ou não
     * @param ultimosSucessos   as execuções com sucesso mais recentes (no máximo
     *                          {@link #SUCESSOS_VAZIOS_PARA_ALERTA}), mais recente primeiro
     */
    public static SaudeCrawler calcular(boolean ligada, Optional<ExecucaoFonte> ultimaExecucao,
                                        List<ExecucaoFonte> ultimosSucessos) {
        if (!ligada) {
            return DESLIGADA;
        }
        if (ultimaExecucao.isEmpty()) {
            return SEM_DADOS;
        }
        if (ultimaExecucao.get().getStatus() == StatusFonte.ERRO) {
            return FALHA;
        }
        boolean sucessosVazios = ultimosSucessos.size() >= SUCESSOS_VAZIOS_PARA_ALERTA
                && ultimosSucessos.stream().limit(SUCESSOS_VAZIOS_PARA_ALERTA).allMatch(r -> r.getEncontradas() == 0);
        return sucessosVazios ? ALERTA : OK;
    }
}
