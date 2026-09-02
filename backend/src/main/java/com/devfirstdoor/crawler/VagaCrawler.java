package com.devfirstdoor.crawler;

import com.devfirstdoor.domain.Vaga;

import java.util.List;

public interface VagaCrawler {

    /**
     * Nome curto e estável da fonte (ex: "GUPY"), usado em logs e no campo Vaga.fonte.
     */
    String getFonte();

    List<Vaga> coletar();
}
