package com.devfirstdoor.crawler.gupy;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class GupyJobPageParserTest {

    private final GupyJobPageParser parser = new GupyJobPageParser(JsonMapper.builder().build());

    @Test
    void extrairTexto_deveJuntarDescricaoRequisitosESkillsSemHtml() {
        String html = """
                <html><body><div id="__next"></div>
                <script id="__NEXT_DATA__" type="application/json">
                {"props":{"pageProps":{"job":{
                  "name":"Estágio em Desenvolvimento",
                  "description":"<p>Venha para o <strong>time</strong>!</p>",
                  "responsibilities":"<ul><li>Manter APIs</li></ul>",
                  "prerequisites":"<ul><li>Conhecimento em Java e Spring</li></ul>",
                  "relevantExperiences":null,
                  "skills":[{"name":"SQL"}]
                }}}}
                </script></body></html>
                """;

        String texto = parser.extrairTexto(html);

        assertThat(texto).contains("Estágio em Desenvolvimento", "Venha para o time!", "Manter APIs",
                "Conhecimento em Java e Spring", "SQL");
        assertThat(texto).doesNotContain("<p>", "<li>");
    }

    @Test
    void extrairTexto_semNextData_deveDevolverNull() {
        assertThat(parser.extrairTexto("<html><body>Página fora do ar</body></html>")).isNull();
    }
}
