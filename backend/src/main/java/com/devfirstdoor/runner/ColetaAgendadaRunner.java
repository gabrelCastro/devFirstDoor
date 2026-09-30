package com.devfirstdoor.runner;

import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.service.ColetaService;
import com.devfirstdoor.service.PausaAgendamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

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
    private final PausaAgendamento pausa;
    private final Duration intervalo;
    private volatile LocalDateTime proximaColeta;

    public ColetaAgendadaRunner(ColetaService coletaService, PausaAgendamento pausa,
                                @Value("${app.crawler.intervalo:6h}") Duration intervalo) {
        this.coletaService = coletaService;
        this.pausa = pausa;
        this.intervalo = intervalo;
        this.proximaColeta = LocalDateTime.now().plus(intervalo);
    }

    @Scheduled(fixedDelayString = "${app.crawler.intervalo:6h}", initialDelayString = "${app.crawler.intervalo:6h}")
    public void coletar() {
        try {
            if (pausa.isPausado()) {
                log.info("Coleta agendada pulada: agendamento pausado pelo painel admin");
                return;
            }
            log.info("Iniciando coleta agendada de vagas...");
            coletaService.executarTodos(OrigemColeta.AGENDADA);
        } finally {
            // fixedDelay: a próxima conta a partir do fim desta
            proximaColeta = LocalDateTime.now().plus(intervalo);
        }
    }

    /** Horário previsto da próxima coleta agendada, para o painel admin. */
    public LocalDateTime getProximaColeta() {
        return proximaColeta;
    }
}
