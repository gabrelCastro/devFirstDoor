package com.devfirstdoor.crawler.linkedin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LinkedinHtmlClientTest {

    @Test
    void montarUrl_deveCodificarTermoEIncluirLocalizacaoRemotoEOffset() {
        LinkedinHtmlClient client = new LinkedinHtmlClient(new LinkedinCrawlerProperties());

        assertThat(client.montarUrl("estágio desenvolvimento", 10)).isEqualTo(
                "https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search"
                        + "?keywords=est%C3%A1gio+desenvolvimento&geoId=106057199&f_WT=2&start=10");
    }

    @Test
    void montarUrl_semApenasRemoto_naoDeveEnviarFiltroDeModalidade() {
        LinkedinCrawlerProperties properties = new LinkedinCrawlerProperties();
        properties.setApenasRemoto(false);

        assertThat(new LinkedinHtmlClient(properties).montarUrl("java", 0)).doesNotContain("f_WT");
    }
}
