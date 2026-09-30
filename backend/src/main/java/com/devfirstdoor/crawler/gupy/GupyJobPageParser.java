package com.devfirstdoor.crawler.gupy;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * A API do portal só traz título e tipo de contrato; os requisitos (onde aparece
 * a linguagem) ficam na página da vaga, embutidos no JSON {@code __NEXT_DATA__}
 * do Next.js em {@code props.pageProps.job}.
 */
public class GupyJobPageParser {

    private static final List<String> CAMPOS_TEXTO = List.of(
            "name", "description", "responsibilities", "prerequisites", "relevantExperiences");

    private final ObjectMapper objectMapper;

    public GupyJobPageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Texto (sem HTML) de título, descrição, requisitos e skills da vaga, ou null se a página não tem os dados. */
    public String extrairTexto(String html) {
        Element script = Jsoup.parse(html).selectFirst("script#__NEXT_DATA__");
        if (script == null) {
            return null;
        }
        JsonNode job = objectMapper.readTree(script.data()).path("props").path("pageProps").path("job");
        if (job.isMissingNode()) {
            return null;
        }

        StringBuilder texto = new StringBuilder();
        for (String campo : CAMPOS_TEXTO) {
            Document fragmento = Jsoup.parseBodyFragment(job.path(campo).asString(""));
            texto.append(fragmento.text()).append('\n');
        }
        for (JsonNode skill : job.path("skills")) {
            texto.append(skill.isString() ? skill.asString() : skill.path("name").asString("")).append('\n');
        }
        return texto.toString();
    }
}
