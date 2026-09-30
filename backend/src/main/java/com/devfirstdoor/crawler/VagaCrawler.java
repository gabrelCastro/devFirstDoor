package com.devfirstdoor.crawler;

import com.devfirstdoor.domain.Vaga;

import java.util.List;

public interface VagaCrawler {

    /**
     * Nome curto e estável da fonte (ex: "GUPY"), usado em logs e no campo Vaga.fonte.
     */
    String getFonte();

    List<Vaga> coletar();

    /**
     * Igual a {@link #coletar()}, informando o andamento para o painel admin. Crawlers
     * demorados (LinkedIn, Gupy) sobrescrevem; os demais ignoram o progresso.
     */
    default List<Vaga> coletar(ProgressoColeta progresso) {
        return coletar();
    }

    /**
     * Se a fonte tem o que coletar com a configuração atual. Greenhouse e Lever sem
     * empresas cadastradas ficam desligados (o LinkedIn desligado nem vira bean).
     */
    default boolean isLigada() {
        return true;
    }

    /**
     * Quantas vagas a última chamada a {@link #coletar()} encontrou na fonte ao todo, para o
     * histórico de execuções. Normalmente são as próprias devolvidas; crawlers que pulam vagas
     * já salvas antes de devolvê-las (LinkedIn) somam essas aqui, senão "0 encontradas" não
     * distinguiria uma fonte sem nada novo de uma fonte quebrada.
     */
    default int contarEncontradas(List<Vaga> devolvidas) {
        return devolvidas.size();
    }
}
