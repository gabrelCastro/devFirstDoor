package com.devfirstdoor.runner;

import com.devfirstdoor.service.ColetaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Repete a coleta a cada app.crawler.intervalo (padrão 6h) para o banco não ficar
 * parado na foto tirada no startup. O primeiro disparo espera um intervalo inteiro,
 * já que a coleta inicial cobre o momento em que a aplicação sobe.
 *
 * Desligado nos testes (app.crawler.agendamento.enabled=false) para não depender
 * de rede externa ao rodar "mvn test".
 */
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "app.crawler.agendamento", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ColetaAgendadaRunner {

    private static final Logger log = LoggerFactory.getLogger(ColetaAgendadaRunner.class);

    private final ColetaService coletaService;

    public ColetaAgendadaRunner(ColetaService coletaService) {
        this.coletaService = coletaService;
    }

    @Scheduled(fixedDelayString = "${app.crawler.intervalo:6h}", initialDelayString = "${app.crawler.intervalo:6h}")
    public void coletar() {
        log.info("Iniciando coleta agendada de vagas...");
        coletaService.executarTodos();
    }
}
