package com.devfirstdoor.service;

import com.devfirstdoor.crawler.ProgressoColeta;
import com.devfirstdoor.domain.OrigemColeta;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Estado em memória da coleta em andamento, para o painel admin acompanhar ao vivo. O
 * histórico no banco só recebe cada fonte quando ela termina; isto aqui mostra o que
 * está acontecendo agora (fonte atual e progresso informado pelo crawler).
 *
 * O estado é um registro imutável trocado por inteiro, então quem lê nunca vê uma
 * mistura de duas atualizações.
 */
@Component
public class AndamentoColeta implements ProgressoColeta {

    private final AtomicReference<Estado> atual = new AtomicReference<>();

    public void iniciar(OrigemColeta origem) {
        atual.set(new Estado(origem, LocalDateTime.now(), null, null));
    }

    public void iniciarFonte(String fonte) {
        atual.updateAndGet(e -> e == null ? null : new Estado(e.origem(), e.inicio(), fonte, null));
    }

    @Override
    public void informar(String texto) {
        atual.updateAndGet(e -> e == null ? null : new Estado(e.origem(), e.inicio(), e.fonteAtual(), texto));
    }

    public void finalizar() {
        atual.set(null);
    }

    /** Vazio quando nenhuma coleta está rodando. */
    public Optional<Estado> atual() {
        return Optional.ofNullable(atual.get());
    }

    public record Estado(OrigemColeta origem, LocalDateTime inicio, String fonteAtual, String progresso) {
    }
}
