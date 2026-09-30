package com.devfirstdoor.domain;

import com.devfirstdoor.util.TextNormalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "vaga_descartada", uniqueConstraints =
        @UniqueConstraint(name = "uk_descarte_link_motivo", columnNames = {"link", "motivo"}))
public class VagaDescartada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fonte;

    private String titulo;

    private String empresa;

    private String local;

    @Column(nullable = false, length = 1024)
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoDescarte motivo;

    @Column(nullable = false)
    private LocalDateTime dataDescarte;

    @Column(nullable = false, length = 2048)
    private String textoBusca;

    protected VagaDescartada() {
    }

    public VagaDescartada(String fonte, String titulo, String empresa, String local, String link,
                          MotivoDescarte motivo, LocalDateTime dataDescarte) {
        atualizar(fonte, titulo, empresa, local, dataDescarte);
        this.link = link;
        this.motivo = motivo;
    }

    /** Uma nova ocorrência renova a retenção e os dados que podem ter mudado na fonte. */
    public void atualizar(String fonte, String titulo, String empresa, String local, LocalDateTime dataDescarte) {
        this.fonte = fonte;
        this.titulo = titulo;
        this.empresa = empresa;
        this.local = local;
        this.dataDescarte = dataDescarte;
        this.textoBusca = TextNormalizer.normalizar(String.join(" ",
                texto(titulo), texto(empresa), texto(local), texto(link)));
    }

    private static String texto(String valor) {
        return valor != null ? valor : "";
    }

    public Long getId() {
        return id;
    }

    public String getFonte() {
        return fonte;
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

    public String getLink() {
        return link;
    }

    public MotivoDescarte getMotivo() {
        return motivo;
    }

    public LocalDateTime getDataDescarte() {
        return dataDescarte;
    }
}
