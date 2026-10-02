package com.devfirstdoor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {

    private String usuario = "admin";

    /**
     * Senha do admin criado/sincronizado no banco ao subir. Sem senha (o padrão) nenhum admin é
     * criado a partir da configuração: não existe senha padrão.
     */
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
