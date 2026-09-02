package com.devfirstdoor.crawler.remoteok;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.crawler.remoteok")
public class RemoteOkCrawlerProperties {

    /** URL base da API pública da RemoteOK. */
    private String baseUrl = "https://remoteok.com";

    /**
     * Endpoints combinados para ampliar a cobertura — a RemoteOK expõe o mesmo
     * catálogo através de vários recortes por tag, com bastante sobreposição
     * entre eles (a deduplicação por id cuida da sobreposição).
     */
    private List<String> feeds = List.of(
            "/api",
            "/remote-dev-jobs.json",
            "/remote-internship-jobs.json",
            "/remote-junior-jobs.json"
    );

    /** Pausa entre requisições HTTP, para não sobrecarregar o site (rate limiting básico). */
    private long requestDelayMs = 1000;

    /**
     * As tags da RemoteOK são autodeclaradas pelo anunciante e, na prática, vêm
     * bastante genéricas/repetidas entre vagas de áreas completamente diferentes
     * (ex: "Room Attendant" marcada como "junior" e "internship"). Por isso a
     * classificação usa só o título da vaga (campo "position"), nunca as tags.
     */
    private List<String> palavrasEstagio = List.of("intern", "internship", "trainee");

    private List<String> palavrasJunior = List.of("junior", "jr");

    private List<String> palavrasTech = List.of(
            "java", "javascript", "python", "developer", "engineer", "engineering", "software",
            "backend", "frontend", "full stack", "fullstack", "devops", "programming",
            "react", "node", "qa", "sql", "cloud", "android", "ios",
            "golang", "php", "ruby", "kotlin", "swift", "typescript",
            "data scientist", "data analyst", "data engineer"
    );

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<String> getFeeds() {
        return feeds;
    }

    public void setFeeds(List<String> feeds) {
        this.feeds = feeds;
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
}
