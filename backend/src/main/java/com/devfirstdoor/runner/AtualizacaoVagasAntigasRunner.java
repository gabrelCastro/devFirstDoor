package com.devfirstdoor.runner;

import com.devfirstdoor.service.ConsultaVagasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Bancos criados antes dos filtros da API têm vagas sem as colunas remoto,
 * internacional e texto de busca. Roda antes da coleta inicial e é barato
 * quando não há nada pendente (só uma consulta).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AtualizacaoVagasAntigasRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AtualizacaoVagasAntigasRunner.class);

    private final ConsultaVagasService consultaVagasService;

    public AtualizacaoVagasAntigasRunner(ConsultaVagasService consultaVagasService) {
        this.consultaVagasService = consultaVagasService;
    }

    @Override
    public void run(String... args) {
        int atualizadas = consultaVagasService.preencherCamposDerivadosPendentes();
        if (atualizadas > 0) {
            log.info("Campos de filtro preenchidos em {} vagas antigas", atualizadas);
        }
    }
}
