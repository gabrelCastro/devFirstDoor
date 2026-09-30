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

    // Colunas derivadas (ver ClassificacaoVaga). Anuláveis para o "ddl-auto: update"
    // conseguir adicioná-las em bancos com vagas antigas, preenchidas depois no startup.
    private Boolean remoto;

    private Boolean internacional;

    @Column(length = 2048)
    private String textoBusca;

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
        atualizarCamposDerivados();
    }

    public void atualizarCamposDerivados() {
        this.remoto = ClassificacaoVaga.isRemoto(local);
        this.internacional = ClassificacaoVaga.isInternacional(fonte, local);
        this.textoBusca = ClassificacaoVaga.textoDeBusca(titulo, empresa, local);
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

    public boolean isRemoto() {
        return Boolean.TRUE.equals(remoto);
    }

    public boolean isInternacional() {
        return Boolean.TRUE.equals(internacional);
    }

    public String getTextoBusca() {
        return textoBusca;
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
