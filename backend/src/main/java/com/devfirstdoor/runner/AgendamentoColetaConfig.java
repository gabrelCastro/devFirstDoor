package com.devfirstdoor.runner;

import com.devfirstdoor.service.ConfiguracaoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Recalcula o intervalo no fim de cada execução para aceitar alterações sem reiniciar. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.crawler.agendamento", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgendamentoColetaConfig implements SchedulingConfigurer {

    private final ColetaAgendadaRunner runner;
    private final ConfiguracaoService configuracaoService;

    public AgendamentoColetaConfig(ColetaAgendadaRunner runner, ConfiguracaoService configuracaoService) {
        this.runner = runner;
        this.configuracaoService = configuracaoService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(runner::coletar, this::proximaExecucao);
    }

    Instant proximaExecucao(TriggerContext contexto) {
        Instant referencia = contexto.lastCompletion() != null
                ? contexto.lastCompletion()
                : contexto.getClock().instant();
        Instant proxima = referencia.plus(configuracaoService.obter().intervaloColeta());
        runner.definirProximaColeta(LocalDateTime.ofInstant(proxima, ZoneId.systemDefault()));
        return proxima;
    }
}
