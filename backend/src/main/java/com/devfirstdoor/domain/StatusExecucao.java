package com.devfirstdoor.domain;

/**
 * Status geral de uma execução de coleta. PARCIAL = pelo menos uma fonte falhou e outra
 * funcionou. Uma execução interrompida (aplicação derrubada no meio) fica EM_ANDAMENTO.
 */
public enum StatusExecucao {
    EM_ANDAMENTO,
    SUCESSO,
    PARCIAL,
    ERRO
}
