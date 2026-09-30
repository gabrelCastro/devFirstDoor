package com.devfirstdoor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Valor administrativo persistido como JSON para permitir tipos diferentes por chave. */
@Entity
@Table(name = "configuracao")
public class Configuracao {

    @Id
    @Column(length = 100)
    private String chave;

    @Column(name = "valor_json", nullable = false, length = 10_000)
    private String valorJson;

    protected Configuracao() {
    }

    public Configuracao(String chave, String valorJson) {
        this.chave = chave;
        this.valorJson = valorJson;
    }

    public String getChave() {
        return chave;
    }

    public String getValorJson() {
        return valorJson;
    }
}
