package com.devfirstdoor.crawler.linkedin;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LinkedinJobParserTest {

    private static final String BASE_URL = "https://www.linkedin.com";

    private final LinkedinJobParser parser = new LinkedinJobParser();

    // Estrutura real da busca pública (jobs-guest), com classes utilitárias reduzidas.
    private static final String HTML = """
            <!DOCTYPE html>
            <li>
              <div class="base-card base-search-card job-search-card" data-entity-urn="urn:li:jobPosting:4465399802">
                <a class="base-card__full-link" href="https://www.linkedin.com/jobs/view/estagio-em-desenvolvimento-de-software-at-acme-4465399802?position=1&amp;pageNum=0&amp;refId=abc&amp;trackingId=xyz">
                  <span class="sr-only">Estágio em Desenvolvimento de Software</span>
                </a>
                <div class="base-search-card__info">
                  <h3 class="base-search-card__title">
                    Estágio em Desenvolvimento de Software
                  </h3>
                  <h4 class="base-search-card__subtitle">
                    <a class="hidden-nested-link" href="https://www.linkedin.com/company/acme">ACME</a>
                  </h4>
                  <div class="base-search-card__metadata">
                    <span class="job-search-card__location">São Paulo, SP</span>
                    <time class="job-search-card__listdate" datetime="2026-09-10">2 semanas atrás</time>
                  </div>
                </div>
              </div>
            </li>
            <li>
              <div class="base-card base-search-card job-search-card">
                <a class="base-card__full-link" href="https://www.linkedin.com/jobs/view/desenvolvedor-junior-at-foo-123?position=2&amp;refId=abc">
                  <span class="sr-only">Desenvolvedor Júnior</span>
                </a>
                <div class="base-search-card__info">
                  <h3 class="base-search-card__title">Desenvolvedor Júnior</h3>
                  <h4 class="base-search-card__subtitle"><a>Foo</a></h4>
                  <div class="base-search-card__metadata">
                    <time class="job-search-card__listdate--new" datetime="data-invalida">1 dia atrás</time>
                  </div>
                </div>
              </div>
            </li>
            <li>
              <div class="base-card base-search-card job-search-card" data-entity-urn="urn:li:jobPosting:999">
                <div class="base-search-card__info">
                  <h3 class="base-search-card__title">Vaga sem empresa</h3>
                </div>
              </div>
            </li>
            """;

    @Test
    void parse_deveExtrairCamposEMontarLinkEstavelPeloIdDaVaga() {
        List<LinkedinJobDto> jobs = parser.parse(Jsoup.parse(HTML), BASE_URL);

        assertThat(jobs).hasSize(2);
        LinkedinJobDto job = jobs.get(0);
        assertThat(job.id()).isEqualTo("4465399802");
        assertThat(job.titulo()).isEqualTo("Estágio em Desenvolvimento de Software");
        assertThat(job.empresa()).isEqualTo("ACME");
        assertThat(job.local()).isEqualTo("São Paulo, SP");
        assertThat(job.link()).isEqualTo("https://www.linkedin.com/jobs/view/4465399802");
        assertThat(job.dataPublicacao()).isEqualTo(LocalDate.of(2026, 9, 10));
    }

    @Test
    void parse_semIdDaVaga_deveUsarHrefSemParametrosDeRastreamento() {
        LinkedinJobDto job = parser.parse(Jsoup.parse(HTML), BASE_URL).get(1);

        assertThat(job.id()).isNull();
        assertThat(job.link()).isEqualTo("https://www.linkedin.com/jobs/view/desenvolvedor-junior-at-foo-123");
        assertThat(job.local()).isNull();
        assertThat(job.dataPublicacao()).isNull();
    }

    @Test
    void parseDescricao_deveExtrairOTextoDaDescricaoDaPaginaDeDetalhe() {
        Document doc = Jsoup.parse("""
                <section class="description">
                  <div class="description__text description__text--rich">
                    <section class="show-more-less-html">
                      <div class="show-more-less-html__markup">
                        <p>Modelo de trabalho: <strong>100% presencial</strong>.</p>
                      </div>
                    </section>
                  </div>
                  <ul class="description__job-criteria-list"><li>Nível de experiência: Assistente</li></ul>
                </section>
                """);

        assertThat(parser.parseDescricao(doc)).isEqualTo("Modelo de trabalho: 100% presencial.");
    }

    @Test
    void parse_devolveListaVaziaQuandoNaoHaCards() {
        Document doc = Jsoup.parse("<!DOCTYPE html>");

        assertThat(parser.parse(doc, BASE_URL)).isEmpty();
    }
}
