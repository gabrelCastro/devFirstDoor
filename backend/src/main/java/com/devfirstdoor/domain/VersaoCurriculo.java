package com.devfirstdoor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Um currículo adaptado para uma vaga. Guarda uma cópia do perfil do momento (a versão continua
 * reproduzível se o perfil mudar), a análise da vaga, a proposta validada e as escolhas da pessoa.
 * Pode estar ligada a uma candidatura, para saber qual currículo foi enviado para qual vaga.
 */
@Entity
@Table(name = "versao_curriculo")
public class VersaoCurriculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidatura_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Candidatura candidatura;

    @Column(nullable = false, length = 300)
    private String titulo;

    @Column(name = "descricao_vaga", nullable = false, length = 20_000)
    private String descricaoVaga;

    @Column(name = "perfil_json", nullable = false, length = 200_000)
    private String perfilJson;

    @Column(name = "vaga_json", nullable = false, length = 50_000)
    private String vagaJson;

    @Column(name = "proposta_json", nullable = false, length = 200_000)
    private String propostaJson;

    @Column(name = "escolhas_json", nullable = false, length = 20_000)
    private String escolhasJson;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    protected VersaoCurriculo() {
    }

    public VersaoCurriculo(Usuario usuario, Candidatura candidatura, String titulo, String descricaoVaga,
                           String perfilJson, String vagaJson, String propostaJson, String escolhasJson,
                           LocalDateTime agora) {
        this.usuario = usuario;
        this.candidatura = candidatura;
        this.titulo = titulo;
        this.descricaoVaga = descricaoVaga;
        this.perfilJson = perfilJson;
        this.vagaJson = vagaJson;
        this.propostaJson = propostaJson;
        this.escolhasJson = escolhasJson;
        this.criadaEm = agora;
        this.atualizadaEm = agora;
    }

    public void atualizar(String propostaJson, String escolhasJson, LocalDateTime agora) {
        this.propostaJson = propostaJson;
        this.escolhasJson = escolhasJson;
        this.atualizadaEm = agora;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Candidatura getCandidatura() {
        return candidatura;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricaoVaga() {
        return descricaoVaga;
    }

    public String getPerfilJson() {
        return perfilJson;
    }

    public String getVagaJson() {
        return vagaJson;
    }

    public String getPropostaJson() {
        return propostaJson;
    }

    public String getEscolhasJson() {
        return escolhasJson;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getAtualizadaEm() {
        return atualizadaEm;
    }
}
