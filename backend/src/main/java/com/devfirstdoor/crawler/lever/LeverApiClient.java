package com.devfirstdoor.crawler.lever;

import com.devfirstdoor.crawler.ConsultaBoard;
import com.devfirstdoor.crawler.lever.dto.LeverPostingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class LeverApiClient {

    private static final Logger log = LoggerFactory.getLogger(LeverApiClient.class);
    private static final String USER_AGENT = "dev-first-door/1.0 (+https://github.com/; uso educacional/coleta de vagas)";

    private final RestClient restClient;
    private final LeverCrawlerProperties properties;

    public LeverApiClient(LeverCrawlerProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }

    /**
     * Todas as vagas publicadas pela empresa (sem "limit" a API devolve tudo de uma
     * vez), ou lista vazia se a requisição falhar (empresa inexistente, API fora do ar).
     */
    public List<LeverPostingDto> buscarVagas(String empresa) {
        try {
            return buscarBoard(empresa).vagas();
        } catch (Exception e) {
            log.error("Falha ao buscar vagas no Lever (empresa '{}'): {}", empresa, e.getMessage());
            return List.of();
        }
    }

    /** Consulta usada pelo painel, que precisa diferenciar 404 de um board existente e vazio. */
    public ConsultaBoard<LeverPostingDto> buscarBoard(String empresa) {
        aguardarEntreRequisicoes();
        try {
            List<LeverPostingDto> resposta = restClient.get()
                    .uri("/v0/postings/{empresa}?mode=json", empresa)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<LeverPostingDto>>() {
                    });
            return ConsultaBoard.existente(resposta);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                return ConsultaBoard.inexistente();
            }
            throw e;
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
