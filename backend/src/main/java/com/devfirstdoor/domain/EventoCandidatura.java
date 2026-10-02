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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/** Linha do tempo de uma candidatura: criação, mudanças de etapa e notas do usuário. */
@Entity
@Table(name = "evento_candidatura")
public class EventoCandidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Candidatura candidatura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoEventoCandidatura tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_anterior", length = 20)
    private EtapaCandidatura etapaAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_nova", length = 20)
    private EtapaCandidatura etapaNova;

    @Column(length = 2000)
    private String texto;

    @Column(nullable = false)
    private LocalDateTime data;

    protected EventoCandidatura() {
    }

    private EventoCandidatura(Candidatura candidatura, TipoEventoCandidatura tipo, EtapaCandidatura etapaAnterior,
                              EtapaCandidatura etapaNova, String texto, LocalDateTime data) {
        this.candidatura = candidatura;
        this.tipo = tipo;
        this.etapaAnterior = etapaAnterior;
        this.etapaNova = etapaNova;
        this.texto = texto;
        this.data = data;
    }

    public static EventoCandidatura criada(Candidatura candidatura, LocalDateTime data) {
        return new EventoCandidatura(candidatura, TipoEventoCandidatura.CRIADA, null, candidatura.getEtapa(), null, data);
    }

    public static EventoCandidatura etapa(Candidatura candidatura, EtapaCandidatura anterior, LocalDateTime data) {
        return new EventoCandidatura(candidatura, TipoEventoCandidatura.ETAPA, anterior, candidatura.getEtapa(), null, data);
    }

    public static EventoCandidatura nota(Candidatura candidatura, String texto, LocalDateTime data) {
        return new EventoCandidatura(candidatura, TipoEventoCandidatura.NOTA, null, null, texto, data);
    }

    public Long getId() {
        return id;
    }

    public Candidatura getCandidatura() {
        return candidatura;
    }

    public TipoEventoCandidatura getTipo() {
        return tipo;
    }

    public EtapaCandidatura getEtapaAnterior() {
        return etapaAnterior;
    }

    public EtapaCandidatura getEtapaNova() {
        return etapaNova;
    }

    public String getTexto() {
        return texto;
    }

    public LocalDateTime getData() {
        return data;
    }
}
