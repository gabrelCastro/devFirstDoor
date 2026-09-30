package com.devfirstdoor.controller.dto;

import com.devfirstdoor.repository.VagaFiltro.Escopo;
import com.devfirstdoor.repository.VagaFiltro.Secao;

import java.util.List;
import java.util.Map;

/**
 * Contagens das abas: as de seção respeitam a abrangência e a busca atuais, e
 * as de abrangência respeitam a seção e a busca atuais. "total" e "fontes" são
 * do banco inteiro, sem filtro.
 */
public record ContagensResponse(
        long total,
        List<String> fontes,
        Map<Secao, Long> secao,
        Map<Escopo, Long> escopo
) {
}
