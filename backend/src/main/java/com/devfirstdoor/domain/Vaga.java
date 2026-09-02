package com.devfirstdoor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "vaga")
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String empresa;

    private String local;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelVaga nivel;

    @Column(nullable = false, unique = true, length = 1024)
    private String link;

    @Column(nullable = false)
    private String fonte;

    private LocalDate dataPublicacao;

    @Column(nullable = false)
    private LocalDateTime dataColeta;

    @Column(nullable = false, unique = true, length = 64)
    private String hashDeduplicacao;

    protected Vaga() {
    }

    public Vaga(String titulo, String empresa, String local, NivelVaga nivel, String link,
                String fonte, LocalDate dataPublicacao, LocalDateTime dataColeta) {
        this.titulo = titulo;
        this.empresa = empresa;
        this.local = local;
        this.nivel = nivel;
        this.link = link;
        this.fonte = fonte;
        this.dataPublicacao = dataPublicacao;
        this.dataColeta = dataColeta;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getEmpresa() {
        return empresa;
    }

    public String getLocal() {
        return local;
    }

    public NivelVaga getNivel() {
        return nivel;
    }

    public String getLink() {
        return link;
    }

    public String getFonte() {
        return fonte;
    }

    public LocalDate getDataPublicacao() {
        return dataPublicacao;
    }

    public LocalDateTime getDataColeta() {
        return dataColeta;
    }

    public String getHashDeduplicacao() {
        return hashDeduplicacao;
    }

    public void setHashDeduplicacao(String hashDeduplicacao) {
        this.hashDeduplicacao = hashDeduplicacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vaga vaga)) return false;
        return Objects.equals(link, vaga.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(link);
    }
}
