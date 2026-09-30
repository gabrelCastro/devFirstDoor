package com.devfirstdoor.crawler.gupy;

import com.devfirstdoor.crawler.gupy.dto.GupyJobDto;
import com.devfirstdoor.crawler.gupy.dto.GupyJobsPageDto;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Component
public class GupyApiClient {

    private static final Logger log = LoggerFactory.getLogger(GupyApiClient.class);
    private static final String USER_AGENT = "dev-first-door/1.0 (+https://github.com/; uso educacional/coleta de vagas)";

    private final RestClient restClient;
    private final GupyCrawlerProperties properties;
    private final GupyJobPageParser jobPageParser;

    public GupyApiClient(GupyCrawlerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.jobPageParser = new GupyJobPageParser(objectMapper);
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }

    /**
     * Busca todas as páginas de vagas para um termo de busca, respeitando o limite
     * de páginas e a pausa entre requisições configurados.
     */
    public List<GupyJobDto> buscarTodasAsPaginas(String termoBusca) {
        List<GupyJobDto> resultado = new ArrayList<>();
        int offset = 0;
        int pageSize = properties.getPageSize();

        for (int pagina = 0; pagina < properties.getMaxPaginasPorTermo(); pagina++) {
            GupyJobsPageDto page = buscarPagina(termoBusca, offset, pageSize);
            if (page == null || page.data() == null || page.data().isEmpty()) {
                break;
            }
            resultado.addAll(page.data());
            if (page.data().size() < pageSize) {
                break;
            }
            offset += pageSize;
            aguardarEntreRequisicoes();
        }
        return resultado;
    }

    /**
     * Título, descrição e requisitos da vaga, lidos da página pública dela, ou null
     * se a página não pôde ser lida.
     */
    public String buscarTextoDaVaga(String jobUrl) {
        aguardarEntreRequisicoes();
        try {
            String html = Jsoup.connect(jobUrl)
                    .userAgent(USER_AGENT)
                    .timeout(10_000)
                    .execute()
                    .body();
            return jobPageParser.extrairTexto(html);
        } catch (Exception e) {
            log.warn("Não foi possível ler a página da vaga {} na Gupy: {}", jobUrl, e.getMessage());
            return null;
        }
    }

    private GupyJobsPageDto buscarPagina(String termoBusca, int offset, int limit) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("jobName", termoBusca)
                            .queryParam("limit", limit)
                            .queryParam("offset", offset)
                            .build())
                    .retrieve()
                    .body(GupyJobsPageDto.class);
        } catch (Exception e) {
            log.error("Falha ao buscar vagas na Gupy para o termo '{}' (offset {}): {}", termoBusca, offset, e.getMessage());
            return null;
        }
    }

    private void aguardarEntreRequisicoes() {
        try {
            Thread.sleep(properties.getRequestDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
