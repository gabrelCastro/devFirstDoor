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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Uma vaga que o usuário acompanha. Guarda uma cópia de título, empresa, local e link: assim a
 * candidatura externa (sem {@code vaga}) funciona igual e o histórico não muda se a vaga mudar.
 */
@Entity
@Table(name = "candidatura", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "vaga_id"}))
public class Candidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    /** Nula nas candidaturas externas. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaga_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Vaga vaga;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String empresa;

    private String local;

    @Column(length = 1024)
    private String link;

    /** Fonte da vaga coletada; nula nas externas. */
    private String fonte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EtapaCandidatura etapa;

    @Column(name = "etapa_desde", nullable = false)
    private LocalDateTime etapaDesde;

    @Column(name = "data_candidatura")
    private LocalDate dataCandidatura;

    @Column(name = "proximo_passo", length = 200)
    private String proximoPasso;

    @Column(name = "data_proximo_passo")
    private LocalDate dataProximoPasso;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    protected Candidatura() {
    }

    private Candidatura(Usuario usuario, Vaga vaga, String titulo, String empresa, String local, String link,
                        String fonte, EtapaCandidatura etapa, LocalDateTime agora) {
        this.usuario = usuario;
        this.vaga = vaga;
        this.titulo = titulo;
        this.empresa = empresa;
        this.local = local;
        this.link = link;
        this.fonte = fonte;
        this.etapa = etapa;
        this.etapaDesde = agora;
        this.criadaEm = agora;
        this.atualizadaEm = agora;
    }

    public static Candidatura daVaga(Usuario usuario, Vaga vaga, LocalDateTime agora) {
        return new Candidatura(usuario, vaga, vaga.getTitulo(), vaga.getEmpresa(), vaga.getLocal(), vaga.getLink(),
                vaga.getFonte(), EtapaCandidatura.INTERESSE, agora);
    }

    public static Candidatura externa(Usuario usuario, String titulo, String empresa, String local, String link,
                                      EtapaCandidatura etapa, LocalDateTime agora) {
        return new Candidatura(usuario, null, titulo, empresa, local, link, null, etapa, agora);
    }

    /** Devolve a etapa anterior. Chegar a uma etapa que implica envio preenche a data, se faltar. */
    public EtapaCandidatura moverPara(EtapaCandidatura nova, LocalDateTime agora) {
        EtapaCandidatura anterior = this.etapa;
        this.etapa = Objects.requireNonNull(nova);
        this.etapaDesde = agora;
        if (nova.impliesEnvio() && dataCandidatura == null) {
            this.dataCandidatura = agora.toLocalDate();
        }
        tocar(agora);
        return anterior;
    }

    public void definirDataCandidatura(LocalDate data) {
        this.dataCandidatura = data;
    }

    public void definirProximoPasso(String texto, LocalDate data) {
        this.proximoPasso = texto;
        this.dataProximoPasso = data;
    }

    public void corrigirDados(String titulo, String empresa, String local, String link) {
        this.titulo = titulo;
        this.empresa = empresa;
        this.local = local;
        this.link = link;
    }

    public void tocar(LocalDateTime agora) {
        this.atualizadaEm = agora;
    }

    public boolean isExterna() {
        return fonte == null;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Vaga getVaga() {
        return vaga;
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

    public String getFonte() {
        return fonte;
    }

    public EtapaCandidatura getEtapa() {
        return etapa;
    }

    public LocalDateTime getEtapaDesde() {
        return etapaDesde;
    }

    public LocalDate getDataCandidatura() {
        return dataCandidatura;
    }

    public String getProximoPasso() {
        return proximoPasso;
    }

    public LocalDate getDataProximoPasso() {
        return dataProximoPasso;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getAtualizadaEm() {
        return atualizadaEm;
    }
}
