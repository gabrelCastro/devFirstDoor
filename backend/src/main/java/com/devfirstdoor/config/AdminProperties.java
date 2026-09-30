package com.devfirstdoor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {

    private String usuario = "admin";

    /** Sem senha (o padrão), a área administrativa fica desligada: não existe senha padrão. */
    private String senha = "";

    public boolean isHabilitado() {
        return senha != null && !senha.isBlank();
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
