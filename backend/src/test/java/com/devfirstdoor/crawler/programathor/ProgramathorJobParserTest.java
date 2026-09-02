package com.devfirstdoor.crawler.programathor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProgramathorJobParserTest {

    private final ProgramathorJobParser parser = new ProgramathorJobParser();

    private static final String HTML = """
            <div class="wrapper-jobs-list">
              <div class="cell-list opacity-60p">
                <a href="/jobs/13029-programador-a-php">
                  <div class="row">
                    <div class="col-sm-3"><div class="cell-logo"></div></div>
                    <div class="col-sm-9">
                      <div class="cell-list-content">
                        <h3 class="color-gray text-24"><span class="text-16 border-red color-red">Vencida</span> Programador(a) PHP</h3>
                        <div class="cell-list-content-icon">
                          <span><i class="fa fa-briefcase"></i>Wase Tecnologia</span>
                          <span><i class="fas fa-map-marker-alt"></i>Remoto</span>
                          <span><i class="far fa-chart-bar"></i>Júnior</span>
                          <span><i class="far fa-file-alt"></i>Estágio</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </a>
              </div>
              <div class="cell-list opacity-60p">
                <a href="/jobs/33528-estagio-de-produto-100-remoto">
                  <div class="row">
                    <div class="col-sm-3"><div class="cell-logo"></div></div>
                    <div class="col-sm-9">
                      <div class="cell-list-content">
                        <h3 class="color-gray text-24">Estágio de Produto 100% Remoto</h3>
                        <div class="cell-list-content-icon">
                          <span><i class="fa fa-briefcase"></i>Empresa XPTO</span>
                          <span><i class="fas fa-map-marker-alt"></i>Remoto</span>
                          <span><i class="far fa-file-alt"></i>Estágio</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </a>
              </div>
            </div>
            """;

    @Test
    void parse_deveDescartarVagaMarcadaComoVencida_eManterAAtiva() {
        Document doc = Jsoup.parse(HTML, "https://programathor.com.br");

        List<ProgramathorJobDto> jobs = parser.parse(doc, "https://programathor.com.br");

        assertThat(jobs).hasSize(1);
        ProgramathorJobDto job = jobs.get(0);
        assertThat(job.titulo()).isEqualTo("Estágio de Produto 100% Remoto");
        assertThat(job.empresa()).isEqualTo("Empresa XPTO");
        assertThat(job.local()).isEqualTo("Remoto");
        assertThat(job.link()).isEqualTo("https://programathor.com.br/jobs/33528-estagio-de-produto-100-remoto");
    }

    @Test
    void parse_devolveListaVaziaQuandoNaoHaCards() {
        Document doc = Jsoup.parse("<div class=\"wrapper-jobs-list\"></div>", "https://programathor.com.br");

        assertThat(parser.parse(doc, "https://programathor.com.br")).isEmpty();
    }
}
