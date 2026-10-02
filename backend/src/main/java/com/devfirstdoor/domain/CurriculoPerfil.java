package com.devfirstdoor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/** Perfil-mestre do currículo de uma conta, guardado como JSON (estrutura em curriculo.PerfilCurriculo). */
@Entity
@Table(name = "curriculo_perfil")
public class CurriculoPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    @Column(name = "perfil_json", nullable = false, length = 200_000)
    private String perfilJson;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected CurriculoPerfil() {
    }

    public CurriculoPerfil(Usuario usuario, String perfilJson, LocalDateTime agora) {
        this.usuario = usuario;
        this.perfilJson = perfilJson;
        this.atualizadoEm = agora;
    }

    public void atualizar(String perfilJson, LocalDateTime agora) {
        this.perfilJson = perfilJson;
        this.atualizadoEm = agora;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getPerfilJson() {
        return perfilJson;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
