package com.devfirstdoor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pausa da coleta agendada, controlada pelo painel admin. Enquanto pausada, o
 * ColetaAgendadaRunner pula os disparos; coletas manuais continuam permitidas.
 *
 * O estado fica na configuração persistida para sobreviver a reinícios.
 */
@Component
public class PausaAgendamento {

    private static final Logger log = LoggerFactory.getLogger(PausaAgendamento.class);

    private final AtomicBoolean pausado = new AtomicBoolean(false);
    private final ConfiguracaoService configuracaoService;

    @Autowired
    public PausaAgendamento(ConfiguracaoService configuracaoService) {
        this.configuracaoService = configuracaoService;
    }

    /** Mantém os testes unitários independentes de JPA. */
    public PausaAgendamento() {
        this.configuracaoService = null;
    }

    public void pausar() {
        if (configuracaoService != null) {
            if (!isPausado()) {
                configuracaoService.definirAgendamentoPausado(true);
                log.info("Coleta agendada pausada pelo painel admin");
            }
        } else if (pausado.compareAndSet(false, true)) {
            log.info("Coleta agendada pausada pelo painel admin");
        }
    }

    public void retomar() {
        if (configuracaoService != null) {
            if (isPausado()) {
                configuracaoService.definirAgendamentoPausado(false);
                log.info("Coleta agendada retomada pelo painel admin");
            }
        } else if (pausado.compareAndSet(true, false)) {
            log.info("Coleta agendada retomada pelo painel admin");
        }
    }

    public boolean isPausado() {
        return configuracaoService != null ? configuracaoService.obter().agendamentoPausado() : pausado.get();
    }
}
