package com.devfirstdoor.crawler.programathor;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.crawler.programathor")
public class ProgramathorCrawlerProperties {

    /** URL base do quadro de vagas (só para programadores) da ProgramaThor. */
    private String baseUrl = "https://programathor.com.br";

    /** Pausa entre requisições HTTP, para não sobrecarregar o site (rate limiting básico). */
    private long requestDelayMs = 1000;

    /** Limite de páginas percorridas por filtro, para evitar coletas sem fim. */
    private int maxPaginasPorFiltro = 5;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getRequestDelayMs() {
        return requestDelayMs;
    }

    public void setRequestDelayMs(long requestDelayMs) {
        this.requestDelayMs = requestDelayMs;
    }

    public int getMaxPaginasPorFiltro() {
        return maxPaginasPorFiltro;
    }

    public void setMaxPaginasPorFiltro(int maxPaginasPorFiltro) {
        this.maxPaginasPorFiltro = maxPaginasPorFiltro;
    }
}
