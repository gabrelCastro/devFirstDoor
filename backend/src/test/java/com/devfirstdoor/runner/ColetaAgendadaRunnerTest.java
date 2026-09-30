package com.devfirstdoor.runner;

import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.service.ColetaService;
import com.devfirstdoor.service.PausaAgendamento;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ColetaAgendadaRunnerTest {

    private final ColetaService coletaService = mock(ColetaService.class);
    private final PausaAgendamento pausa = new PausaAgendamento();
    private final ColetaAgendadaRunner runner = new ColetaAgendadaRunner(coletaService, pausa, Duration.ofHours(6));

    @Test
    void coletar_semPausa_deveRodarAColetaAgendada() {
        runner.coletar();

        verify(coletaService).executarTodos(OrigemColeta.AGENDADA);
    }

    @Test
    void coletar_pausado_devePularAColetaMasSeguirPrevendoAProxima() {
        pausa.pausar();
        LocalDateTime antes = LocalDateTime.now();

        runner.coletar();

        verify(coletaService, never()).executarTodos(any());
        assertThat(runner.getProximaColeta()).isAfterOrEqualTo(antes.plusHours(6));
    }

    @Test
    void coletar_depoisDeRetomar_deveVoltarARodar() {
        pausa.pausar();
        runner.coletar();
        pausa.retomar();
        runner.coletar();

        verify(coletaService).executarTodos(OrigemColeta.AGENDADA);
    }
}
