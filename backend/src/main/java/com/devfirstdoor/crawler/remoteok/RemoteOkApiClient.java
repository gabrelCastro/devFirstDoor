package com.devfirstdoor.crawler.remoteok;

import com.devfirstdoor.crawler.remoteok.dto.RemoteOkJobDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class RemoteOkApiClient {

    private static final Logger log = LoggerFactory.getLogger(RemoteOkApiClient.class);
    private static final String USER_AGENT = "dev-first-door/1.0 (+https://github.com/; uso educacional/coleta de vagas; ver termos da API da Remote OK)";

    private final RestClient restClient;
    private final RemoteOkCrawlerProperties properties;
    private final ObjectMapper objectMapper;

    public RemoteOkApiClient(RemoteOkCrawlerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }

    /**
     * Busca os feeds configurados (recortes por tag do mesmo catálogo, com bastante
     * sobreposição) e devolve a união bruta — a deduplicação por id fica a cargo do
     * chamador. O primeiro elemento de cada resposta é um objeto de termos de uso,
     * sem id/position, e é descartado aqui.
     */
    public List<RemoteOkJobDto> buscarVagas() {
        List<RemoteOkJobDto> resultado = new ArrayList<>();
        for (String feed : properties.getFeeds()) {
            resultado.addAll(buscarFeed(feed));
            aguardarEntreRequisicoes();
        }
        return resultado;
    }

    private List<RemoteOkJobDto> buscarFeed(String feed) {
        try {
            // A RemoteOK responde "Content-Type: application/json" sem charset; lido como
            // bytes e decodificado manualmente em UTF-8 para não corromper acentos/textos
            // não latinos (ex: título/local em árabe) que um content-type sem charset pode
            // fazer o cliente HTTP interpretar como ISO-8859-1 por padrão.
            byte[] corpo = restClient.get()
                    .uri(feed)
                    .retrieve()
                    .body(byte[].class);
            if (corpo == null) {
                return List.of();
            }
            String json = new String(corpo, StandardCharsets.UTF_8);
            List<RemoteOkJobDto> resposta = objectMapper.readValue(json, new TypeReference<List<RemoteOkJobDto>>() {
            });
            return resposta.stream()
                    .filter(job -> job.id() != null && job.position() != null)
                    .toList();
        } catch (Exception e) {
            log.error("Falha ao buscar vagas na RemoteOK (feed '{}'): {}", feed, e.getMessage());
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
