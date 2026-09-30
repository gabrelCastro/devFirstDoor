package com.devfirstdoor.crawler;

/**
 * Por onde um crawler conta ao painel admin em que ponto da coleta está (ex: "termo 3/6").
 * É opcional: quem não informa nada aparece no painel só com o nome da fonte.
 */
@FunctionalInterface
public interface ProgressoColeta {

    /** Para quem chama {@link VagaCrawler#coletar()} sem acompanhar o andamento (ex: testes). */
    ProgressoColeta NENHUM = texto -> {
    };

    void informar(String texto);
}
