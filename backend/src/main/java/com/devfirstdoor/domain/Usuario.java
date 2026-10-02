package com.devfirstdoor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Conta de acesso. A senha fica só como hash (BCrypt, com o prefixo {@code {bcrypt}}). */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Sempre minúsculo: o login não diferencia maiúsculas. */
    @Column(nullable = false, unique = true, length = 40)
    private String login;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Papel papel;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Usuario() {
    }

    public Usuario(String login, String senhaHash, Papel papel, LocalDateTime criadoEm) {
        this.login = login;
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.ativo = true;
        this.criadoEm = criadoEm;
    }

    public void trocarSenha(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public void alterarPapel(Papel papel) {
        this.papel = papel;
    }

    public void alterarAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Papel getPapel() {
        return papel;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
