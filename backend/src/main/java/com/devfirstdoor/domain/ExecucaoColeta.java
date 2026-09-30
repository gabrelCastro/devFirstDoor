package com.devfirstdoor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Uma rodada de coleta (todas as fontes). O resultado de cada fonte fica em {@link ExecucaoFonte}. */
@Entity
@Table(name = "execucao_coleta")
public class ExecucaoColeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemColeta origem;

    @Column(nullable = false)
    private LocalDateTime inicio;

    private LocalDateTime fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusExecucao status;

    protected ExecucaoColeta() {
    }

    public ExecucaoColeta(OrigemColeta origem, LocalDateTime inicio) {
        this.origem = origem;
        this.inicio = inicio;
        this.status = StatusExecucao.EM_ANDAMENTO;
    }

    public void finalizar(LocalDateTime fim, StatusExecucao status) {
        this.fim = fim;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public OrigemColeta getOrigem() {
        return origem;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public StatusExecucao getStatus() {
        return status;
    }
}
