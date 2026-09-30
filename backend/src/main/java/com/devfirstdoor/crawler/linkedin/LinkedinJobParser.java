package com.devfirstdoor.crawler.linkedin;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A busca pública (jobs-guest) devolve só uma lista de {@code <li>}, cada um com um
 * {@code div.base-search-card}. O link do card carrega parâmetros de rastreamento que
 * mudam a cada requisição, então o link salvo é montado a partir do id da vaga
 * ({@code data-entity-urn}) — assim a mesma vaga sempre gera o mesmo link.
 */
public class LinkedinJobParser {

    private static final Pattern JOB_ID = Pattern.compile("jobPosting:(\\d+)");

    public List<LinkedinJobDto> parse(Document document, String baseUrl) {
        List<LinkedinJobDto> jobs = new ArrayList<>();
        for (Element card : document.select("div.base-search-card")) {
            parseCard(card, baseUrl).ifPresent(jobs::add);
        }
        return jobs;
    }

    private Optional<LinkedinJobDto> parseCard(Element card, String baseUrl) {
        String titulo = texto(card, "h3.base-search-card__title");
        String empresa = texto(card, "h4.base-search-card__subtitle");
        String id = id(card);
        String link = link(card, id, baseUrl);
        if (titulo == null || empresa == null || link == null) {
            return Optional.empty();
        }
        String local = texto(card, "span.job-search-card__location");
        return Optional.of(new LinkedinJobDto(id, titulo, empresa, local, link, dataPublicacao(card)));
    }

    /**
     * A página de detalhe da vaga não expõe a modalidade como campo; ela só aparece no
     * texto livre da descrição (ex: "Modelo de trabalho: 100% presencial").
     */
    public String parseDescricao(Document document) {
        Element descricao = document.selectFirst("div.show-more-less-html__markup, div.description__text");
        return descricao != null ? descricao.text() : null;
    }

    private String id(Element card) {
        Matcher matcher = JOB_ID.matcher(card.attr("data-entity-urn"));
        return matcher.find() ? matcher.group(1) : null;
    }

    private String link(Element card, String id, String baseUrl) {
        if (id != null) {
            return baseUrl.replaceAll("/+$", "") + "/jobs/view/" + id;
        }
        Element a = card.selectFirst("a.base-card__full-link[href]");
        return a != null ? a.attr("href").split("\\?")[0] : null;
    }

    private LocalDate dataPublicacao(Element card) {
        Element time = card.selectFirst("time[datetime]");
        if (time == null) {
            return null;
        }
        try {
            return LocalDate.parse(time.attr("datetime"));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String texto(Element card, String seletor) {
        Element el = card.selectFirst(seletor);
        if (el == null) {
            return null;
        }
        String texto = el.text().trim();
        return texto.isEmpty() ? null : texto;
    }
}
