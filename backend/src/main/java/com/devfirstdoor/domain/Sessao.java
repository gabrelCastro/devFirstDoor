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
 * Sessão de login por token. Só o hash SHA-256 do token fica no banco: quem lê a tabela não
 * consegue se passar pelo usuário. Apagar a linha revoga a sessão.
 */
@Entity
@Table(name = "sessao")
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "ultimo_uso", nullable = false)
    private LocalDateTime ultimoUso;

    protected Sessao() {
    }

    public Sessao(Usuario usuario, String tokenHash, LocalDateTime criadaEm, LocalDateTime expiraEm) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
        this.ultimoUso = criadaEm;
    }

    public boolean isExpirada(LocalDateTime agora) {
        return !agora.isBefore(expiraEm);
    }

    public void registrarUso(LocalDateTime agora) {
        this.ultimoUso = agora;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }

    public LocalDateTime getUltimoUso() {
        return ultimoUso;
    }
}
