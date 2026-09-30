package com.devfirstdoor.crawler.gupy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.crawler.gupy")
public class GupyCrawlerProperties {

    private boolean enabled = true;

    /** URL base da API pública de vagas do portal da Gupy. */
    private String baseUrl = "https://employability-portal.gupy.io/api/v1/jobs";

    /** Quantidade de vagas por página (máximo aceito pela API é 100). */
    private int pageSize = 100;

    /** Pausa entre requisições HTTP, para não sobrecarregar o site (rate limiting básico). */
    private long requestDelayMs = 1000;

    /** Limite de páginas percorridas por termo de busca, para evitar coletas sem fim. */
    private int maxPaginasPorTermo = 5;

    /** Se true, só mantém vagas marcadas como remotas (campo isRemoteWork da Gupy). */
    private boolean apenasRemoto = true;

    /** Termos usados na busca (parâmetro jobName) para reduzir o volume trazido pela API. */
    private List<String> termosBusca = List.of(
            "estágio tecnologia",
            "estágio ti",
            "desenvolvedor junior",
            "programador junior",
            "java",
            "analista de sistemas junior"
    );

    /** Palavras que, presentes no título, indicam vaga júnior (quando o tipo de contrato não é estágio). */
    private List<String> palavrasJunior = List.of("júnior", "junior", "jr");

    /** Palavras que, presentes no título, indicam que a vaga é da área de tecnologia. */
    private List<String> palavrasTech = List.of(
            "java", "desenvolv", "program", "software", "sistemas", "dados",
            "devops", "backend", "frontend", "front-end", "full stack", "fullstack",
            "tecnologia da informacao", "suporte tecnico", "infraestrutura de ti",
            "qa", "tester", "dev", "ti"
    );

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

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getRequestDelayMs() {
        return requestDelayMs;
    }

    public void setRequestDelayMs(long requestDelayMs) {
        this.requestDelayMs = requestDelayMs;
    }

    public int getMaxPaginasPorTermo() {
        return maxPaginasPorTermo;
    }

    public void setMaxPaginasPorTermo(int maxPaginasPorTermo) {
        this.maxPaginasPorTermo = maxPaginasPorTermo;
    }

    public boolean isApenasRemoto() {
        return apenasRemoto;
    }

    public void setApenasRemoto(boolean apenasRemoto) {
        this.apenasRemoto = apenasRemoto;
    }

    public List<String> getTermosBusca() {
        return termosBusca;
    }

    public void setTermosBusca(List<String> termosBusca) {
        this.termosBusca = termosBusca;
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
