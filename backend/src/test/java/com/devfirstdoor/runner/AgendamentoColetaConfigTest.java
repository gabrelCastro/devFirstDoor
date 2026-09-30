package com.devfirstdoor.runner;

import com.devfirstdoor.service.ConfiguracaoColeta;
import com.devfirstdoor.service.ConfiguracaoService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TriggerContext;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgendamentoColetaConfigTest {

    @Test
    void proximaExecucao_deveLerOIntervaloAtualEmCadaCalculo() {
        ColetaAgendadaRunner runner = mock(ColetaAgendadaRunner.class);
        ConfiguracaoService configuracaoService = mock(ConfiguracaoService.class);
        when(configuracaoService.obter()).thenReturn(configuracao(Duration.ofMinutes(30)),
                configuracao(Duration.ofMinutes(45)));
        Instant agora = Instant.parse("2026-09-29T12:00:00Z");
        TriggerContext contexto = mock(TriggerContext.class);
        when(contexto.getClock()).thenReturn(Clock.fixed(agora, ZoneOffset.UTC));
        AgendamentoColetaConfig agendamento = new AgendamentoColetaConfig(runner, configuracaoService);

        assertThat(agendamento.proximaExecucao(contexto)).isEqualTo(agora.plus(Duration.ofMinutes(30)));
        assertThat(agendamento.proximaExecucao(contexto)).isEqualTo(agora.plus(Duration.ofMinutes(45)));
    }

    private static ConfiguracaoColeta configuracao(Duration intervalo) {
        return new ConfiguracaoColeta(Map.of(), List.of(), List.of(), List.of(), List.of(),
                intervalo, 7, 3000, 2000, false, false);
    }
}
