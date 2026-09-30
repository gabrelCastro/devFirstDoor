package com.devfirstdoor.runner;

import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.service.ColetaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Roda uma coleta assim que a aplicação sobe, para que "docker compose up"
 * já deixe o banco populado sem nenhum passo manual. Não bloqueia a
 * inicialização do servidor web: roda em uma thread separada.
 *
 * Desligado nos testes (app.crawler.run-on-startup=false) para não depender
 * de rede externa ao rodar "mvn test".
 */
@Component
@ConditionalOnProperty(prefix = "app.crawler", name = "run-on-startup", havingValue = "true", matchIfMissing = true)
public class ColetaInicialRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ColetaInicialRunner.class);

    private final ColetaService coletaService;

    public ColetaInicialRunner(ColetaService coletaService) {
        this.coletaService = coletaService;
    }

    @Override
    public void run(String... args) {
        Thread coleta = new Thread(() -> {
            log.info("Iniciando coleta inicial de vagas...");
            coletaService.executarTodos(OrigemColeta.INICIAL);
        }, "coleta-inicial");
        coleta.setDaemon(true);
        coleta.start();
    }
}
