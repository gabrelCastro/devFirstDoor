package com.devfirstdoor.crawler.programathor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProgramathorHtmlClient {

    private static final Logger log = LoggerFactory.getLogger(ProgramathorHtmlClient.class);
    private static final String USER_AGENT = "dev-first-door/1.0 (+https://github.com/; uso educacional/coleta de vagas)";

    private final ProgramathorCrawlerProperties properties;
    private final ProgramathorJobParser parser = new ProgramathorJobParser();

    public ProgramathorHtmlClient(ProgramathorCrawlerProperties properties) {
        this.properties = properties;
    }

    /**
     * Busca todas as páginas de um filtro (ex: "contract_type=Est%C3%A1gio&remoto=true"),
     * parando quando uma página não traz nenhum card (mesmo vagas vencidas contam para
     * continuar paginando — só páginas de fato vazias encerram a busca).
     */
    public List<ProgramathorJobDto> buscarTodasAsPaginas(String queryString) {
        List<ProgramathorJobDto> resultado = new ArrayList<>();

        for (int pagina = 1; pagina <= properties.getMaxPaginasPorFiltro(); pagina++) {
            Document documento = buscarPagina(queryString, pagina);
            if (documento == null) {
                break;
            }
            Elements cards = documento.select("div.cell-list");
            if (cards.isEmpty()) {
                break;
            }
            resultado.addAll(parser.parse(documento, properties.getBaseUrl()));
            aguardarEntreRequisicoes();
        }
        return resultado;
    }

    private Document buscarPagina(String queryString, int pagina) {
        String url = properties.getBaseUrl().replaceAll("/+$", "") + "/jobs?" + queryString + "&page=" + pagina;
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(10_000)
                    .get();
        } catch (IOException e) {
            log.error("Falha ao buscar vagas na ProgramaThor (filtro '{}', página {}): {}", queryString, pagina, e.getMessage());
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
