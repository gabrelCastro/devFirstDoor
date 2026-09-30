package com.devfirstdoor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pausa da coleta agendada, controlada pelo painel admin. Enquanto pausada, o
 * ColetaAgendadaRunner pula os disparos; coletas manuais continuam permitidas.
 *
 * Fica só em memória: reiniciar a aplicação retoma o agendamento.
 */
@Component
public class PausaAgendamento {

    private static final Logger log = LoggerFactory.getLogger(PausaAgendamento.class);

    private final AtomicBoolean pausado = new AtomicBoolean(false);

    public void pausar() {
        if (pausado.compareAndSet(false, true)) {
            log.info("Coleta agendada pausada pelo painel admin");
        }
    }

    public void retomar() {
        if (pausado.compareAndSet(true, false)) {
            log.info("Coleta agendada retomada pelo painel admin");
        }
    }

    public boolean isPausado() {
        return pausado.get();
    }
}
