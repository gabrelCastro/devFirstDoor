package com.devfirstdoor.crawler.greenhouse;

import com.devfirstdoor.crawler.greenhouse.dto.GreenhouseJobDto;
import com.devfirstdoor.crawler.greenhouse.dto.GreenhouseJobsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class GreenhouseApiClient {

    private static final Logger log = LoggerFactory.getLogger(GreenhouseApiClient.class);
    private static final String USER_AGENT = "dev-first-door/1.0 (+https://github.com/; uso educacional/coleta de vagas)";

    private final RestClient restClient;
    private final GreenhouseCrawlerProperties properties;

    public GreenhouseApiClient(GreenhouseCrawlerProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }

    /**
     * Todas as vagas publicadas no board da empresa, já com a descrição, ou lista vazia
     * se a requisição falhar (board inexistente, API fora do ar).
     */
    public List<GreenhouseJobDto> buscarVagas(String empresa) {
        aguardarEntreRequisicoes();
        try {
            GreenhouseJobsDto resposta = restClient.get()
                    .uri("/v1/boards/{empresa}/jobs?content=true", empresa)
                    .retrieve()
                    .body(GreenhouseJobsDto.class);
            return resposta == null || resposta.jobs() == null ? List.of() : resposta.jobs();
        } catch (Exception e) {
            log.error("Falha ao buscar vagas no Greenhouse (empresa '{}'): {}", empresa, e.getMessage());
            return List.of();
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
