package com.devfirstdoor.crawler.lever;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.crawler.lever")
public class LeverCrawlerProperties {

    private boolean enabled = true;

    /** URL base da Postings API pública do Lever. */
    private String baseUrl = "https://api.lever.co";

    /**
     * Identificadores das empresas (o "{empresa}" em jobs.lever.co/{empresa}). O Lever
     * não tem busca entre empresas, então cada uma precisa ser listada aqui; vazio
     * desliga o crawler.
     */
    private List<String> empresas = List.of();

    /** Pausa entre requisições HTTP, para não sobrecarregar a API (rate limiting básico). */
    private long requestDelayMs = 1000;

    /** Empresas brasileiras e estrangeiras: palavras em português e inglês. */
    private List<String> palavrasEstagio = List.of("estagio", "estagiario", "intern", "internship", "trainee");

    private List<String> palavrasJunior = List.of("junior", "jr");

    private List<String> palavrasTech = List.of(
            "java", "desenvolv", "program", "developer", "engineer", "engineering", "software",
            "backend", "back-end", "frontend", "front-end", "full stack", "fullstack", "devops",
            "sistemas", "dados", "qa", "sql", "cloud", "android", "kotlin",
            "data scientist", "data analyst", "data engineer"
    );

    /** Usadas no local quando a vaga não informa a modalidade ("workplaceType"). */
    private List<String> palavrasRemoto = List.of("remote", "remoto", "anywhere", "home office", "teletrabalho");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<String> getEmpresas() {
        return empresas;
    }

    public void setEmpresas(List<String> empresas) {
        this.empresas = empresas;
    }

    public long getRequestDelayMs() {
        return requestDelayMs;
    }

    public void setRequestDelayMs(long requestDelayMs) {
        this.requestDelayMs = requestDelayMs;
    }

    public List<String> getPalavrasEstagio() {
        return palavrasEstagio;
    }

    public void setPalavrasEstagio(List<String> palavrasEstagio) {
        this.palavrasEstagio = palavrasEstagio;
    }

    public List<String> getPalavrasJunior() {
        return palavrasJunior;
    }

    public void setPalavrasJunior(List<String> palavrasJunior) {
        this.palavrasJunior = palavrasJunior;
    }

    public List<String> getPalavrasTech() {
        return palavrasTech;
    }

    public void setPalavrasTech(List<String> palavrasTech) {
        this.palavrasTech = palavrasTech;
    }

    public List<String> getPalavrasRemoto() {
        return palavrasRemoto;
    }

    public void setPalavrasRemoto(List<String> palavrasRemoto) {
        this.palavrasRemoto = palavrasRemoto;
    }
}
