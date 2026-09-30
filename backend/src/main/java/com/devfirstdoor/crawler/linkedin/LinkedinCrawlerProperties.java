package com.devfirstdoor.crawler.linkedin;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.crawler.linkedin")
public class LinkedinCrawlerProperties {

    /**
     * Desligado por padrão: o robots.txt do LinkedIn proíbe a busca de vagas e este
     * crawler não o consulta. Ligar é uma decisão explícita de quem roda o projeto.
     */
    private boolean enabled = false;

    private String baseUrl = "https://www.linkedin.com";

    /** Localização da busca. 106057199 = Brasil (o parâmetro textual "location" é ignorado para visitantes). */
    private String geoId = "106057199";

    /** Se true, envia f_WT=2 (remoto). Filtro do próprio LinkedIn, aplicado na busca. */
    private boolean apenasRemoto = true;

    /** Pausa mínima entre requisições; um intervalo aleatório de até {@link #requestJitterMs} é somado. */
    private long requestDelayMs = 3000;

    private long requestJitterMs = 2000;

    /** Cada página da busca pública traz ~10 vagas. */
    private int maxPaginasPorTermo = 3;

    private String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36";

    /**
     * O filtro de nível de experiência (f_E) é ignorado na busca pública, então os termos
     * já carregam o nível e a classificação final é feita pelo título.
     */
    private List<String> termosBusca = List.of(
            "estágio java",
            "estagiário java",
            "desenvolvedor java júnior",
            "programador java júnior",
            "java júnior",
            "java jr"
    );

    /** Radicais que indicam estágio no título (cobre estágio, estagiário, estagiária). */
    private List<String> palavrasEstagio = List.of("estagi", "internship");

    private List<String> palavrasJunior = List.of("júnior", "junior", "jr");

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

    public String getGeoId() {
        return geoId;
    }

    public void setGeoId(String geoId) {
        this.geoId = geoId;
    }

    public boolean isApenasRemoto() {
        return apenasRemoto;
    }

    public void setApenasRemoto(boolean apenasRemoto) {
        this.apenasRemoto = apenasRemoto;
    }

    public long getRequestDelayMs() {
        return requestDelayMs;
    }

    public void setRequestDelayMs(long requestDelayMs) {
        this.requestDelayMs = requestDelayMs;
    }

    public long getRequestJitterMs() {
        return requestJitterMs;
    }

    public void setRequestJitterMs(long requestJitterMs) {
        this.requestJitterMs = requestJitterMs;
    }

    public int getMaxPaginasPorTermo() {
        return maxPaginasPorTermo;
    }

    public void setMaxPaginasPorTermo(int maxPaginasPorTermo) {
        this.maxPaginasPorTermo = maxPaginasPorTermo;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public List<String> getTermosBusca() {
        return termosBusca;
    }

    public void setTermosBusca(List<String> termosBusca) {
        this.termosBusca = termosBusca;
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
