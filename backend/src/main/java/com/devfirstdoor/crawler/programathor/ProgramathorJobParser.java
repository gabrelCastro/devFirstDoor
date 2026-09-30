package com.devfirstdoor.crawler.programathor;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cada card de vaga na ProgramaThor é um {@code div.cell-list}. Vagas expiradas
 * ganham um selo "Vencida" dentro do {@code <h3>} do título — essas são descartadas
 * aqui, pois não representam uma posição aberta.
 */
public class ProgramathorJobParser {

    public List<ProgramathorJobDto> parse(Document document, String baseUrl) {
        List<ProgramathorJobDto> jobs = new ArrayList<>();
        Elements cards = document.select("div.cell-list");
        for (Element card : cards) {
            parseCard(card, baseUrl).ifPresent(jobs::add);
        }
        return jobs;
    }

    private Optional<ProgramathorJobDto> parseCard(Element card, String baseUrl) {
        Element h3 = card.selectFirst("h3");
        if (h3 == null || h3.text().toLowerCase().contains("vencida")) {
            return Optional.empty();
        }
        String titulo = h3.text().trim();
        if (titulo.isEmpty()) {
            return Optional.empty();
        }

        Element linkEl = card.selectFirst("a[href]");
        if (linkEl == null) {
            return Optional.empty();
        }
        String href = linkEl.attr("href");
        String link = href.startsWith("http") ? href : baseUrl.replaceAll("/+$", "") + href;

        String empresa = textoDoIcone(card, "fa-briefcase");
        if (empresa == null || empresa.isBlank()) {
            return Optional.empty();
        }
        String local = textoDoIcone(card, "fa-map-marker-alt");

        List<String> tags = card.select("span.tag-list").eachText();

        return Optional.of(new ProgramathorJobDto(titulo, empresa, local, link, tags));
    }

    private String textoDoIcone(Element card, String iconClass) {
        Element span = card.selectFirst("span:has(i." + iconClass + ")");
        return span != null ? span.text().trim() : null;
    }
}
