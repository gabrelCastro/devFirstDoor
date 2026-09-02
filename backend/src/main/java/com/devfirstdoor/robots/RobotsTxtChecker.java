package com.devfirstdoor.robots;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parser simplificado de robots.txt: entende "User-agent" e "Disallow" para o
 * grupo "*", o suficiente para decidir se é seguro raspar um caminho antes de
 * cada execução de crawler. Um robots.txt ausente (404) ou inacessível é
 * tratado como "permite tudo", que é o comportamento padrão do protocolo.
 */
@Component
public class RobotsTxtChecker {

    private static final Logger log = LoggerFactory.getLogger(RobotsTxtChecker.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public boolean isPermitido(String baseUrl, String path) {
        String robotsUrl = baseUrl.replaceAll("/+$", "") + "/robots.txt";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(robotsUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.debug("robots.txt não encontrado em {} (status {}); assumindo scraping permitido", robotsUrl, response.statusCode());
                return true;
            }

            List<String> disallows = extrairDisallowsParaTodos(response.body());
            for (String disallow : disallows) {
                if (!disallow.isEmpty() && path.startsWith(disallow)) {
                    log.warn("robots.txt de {} proíbe o caminho '{}' (regra Disallow: {})", baseUrl, path, disallow);
                    return false;
                }
            }
            return true;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.debug("Não foi possível consultar {} ({}); assumindo scraping permitido", robotsUrl, e.getMessage());
            return true;
        }
    }

    private List<String> extrairDisallowsParaTodos(String robotsTxt) {
        List<String> disallows = new ArrayList<>();
        boolean dentroDoGrupoTodos = false;

        for (String linhaBruta : robotsTxt.split("\n")) {
            String linha = linhaBruta.split("#", 2)[0].trim();
            if (linha.isEmpty()) {
                continue;
            }
            String[] partes = linha.split(":", 2);
            if (partes.length != 2) {
                continue;
            }
            String campo = partes[0].trim().toLowerCase(Locale.ROOT);
            String valor = partes[1].trim();

            if (campo.equals("user-agent")) {
                dentroDoGrupoTodos = valor.equals("*");
            } else if (campo.equals("disallow") && dentroDoGrupoTodos) {
                disallows.add(valor);
            }
        }
        return disallows;
    }
}
