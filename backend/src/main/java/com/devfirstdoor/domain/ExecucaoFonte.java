package com.devfirstdoor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Resultado de uma fonte dentro de uma {@link ExecucaoColeta}. "Encontradas" é o que a
 * fonte tinha de válido (inclusive vagas já salvas), "novas" é o que foi persistido.
 */
@Entity
@Table(name = "execucao_fonte")
public class ExecucaoFonte {

    public static final int TAMANHO_MAXIMO_MENSAGEM = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execucao_id", nullable = false)
    private ExecucaoColeta execucao;

    @Column(nullable = false)
    private String fonte;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusFonte status;

    private int encontradas;

    private int novas;

    private int expiradas;

    @Column(length = TAMANHO_MAXIMO_MENSAGEM)
    private String mensagemErro;

    protected ExecucaoFonte() {
    }

    private ExecucaoFonte(ExecucaoColeta execucao, String fonte, LocalDateTime inicio, LocalDateTime fim,
                          StatusFonte status) {
        this.execucao = execucao;
        this.fonte = fonte;
        this.inicio = inicio;
        this.fim = fim;
        this.status = status;
    }

    public static ExecucaoFonte sucesso(ExecucaoColeta execucao, String fonte, LocalDateTime inicio,
                                        int encontradas, int novas, int expiradas) {
        ExecucaoFonte resultado = new ExecucaoFonte(execucao, fonte, inicio, LocalDateTime.now(), StatusFonte.SUCESSO);
        resultado.encontradas = encontradas;
        resultado.novas = novas;
        resultado.expiradas = expiradas;
        return resultado;
    }

    public static ExecucaoFonte erro(ExecucaoColeta execucao, String fonte, LocalDateTime inicio, String mensagemErro) {
        ExecucaoFonte resultado = new ExecucaoFonte(execucao, fonte, inicio, LocalDateTime.now(), StatusFonte.ERRO);
        resultado.mensagemErro = truncar(mensagemErro);
        return resultado;
    }

    private static String truncar(String mensagem) {
        if (mensagem == null || mensagem.length() <= TAMANHO_MAXIMO_MENSAGEM) {
            return mensagem;
        }
        return mensagem.substring(0, TAMANHO_MAXIMO_MENSAGEM - 3) + "...";
    }

    public Long getId() {
        return id;
    }

    public ExecucaoColeta getExecucao() {
        return execucao;
    }

    public String getFonte() {
        return fonte;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public StatusFonte getStatus() {
        return status;
    }

    public int getEncontradas() {
        return encontradas;
    }

    public int getNovas() {
        return novas;
    }

    public int getExpiradas() {
        return expiradas;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }
}
