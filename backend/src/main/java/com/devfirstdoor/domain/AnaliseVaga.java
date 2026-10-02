package com.devfirstdoor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Cache da análise de uma descrição de vaga, pelo hash do texto normalizado. É compartilhado entre
 * as contas: a mesma vaga colada por duas pessoas é analisada (e paga) uma vez só.
 */
@Entity
@Table(name = "analise_vaga")
public class AnaliseVaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hash_descricao", nullable = false, unique = true, length = 64)
    private String hashDescricao;

    @Column(name = "analise_json", nullable = false, length = 50_000)
    private String analiseJson;

    @Column(nullable = false, length = 100)
    private String modelo;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected AnaliseVaga() {
    }

    public AnaliseVaga(String hashDescricao, String analiseJson, String modelo, LocalDateTime criadaEm) {
        this.hashDescricao = hashDescricao;
        this.analiseJson = analiseJson;
        this.modelo = modelo;
        this.criadaEm = criadaEm;
    }

    public String getHashDescricao() {
        return hashDescricao;
    }

    public String getAnaliseJson() {
        return analiseJson;
    }

    public String getModelo() {
        return modelo;
    }
}
