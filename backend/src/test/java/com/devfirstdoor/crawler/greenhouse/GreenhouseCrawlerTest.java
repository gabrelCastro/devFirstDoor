package com.devfirstdoor.crawler.greenhouse;

import com.devfirstdoor.crawler.greenhouse.dto.GreenhouseJobsDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.robots.RobotsTxtChecker;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * O client e o RobotsTxtChecker são mocks: nenhum teste faz requisição ao Greenhouse.
 * O JSON segue o formato documentado de GET /v1/boards/{empresa}/jobs?content=true.
 */
class GreenhouseCrawlerTest {

    private static final String FIXTURE = """
            {
              "jobs": [
                {
                  "id": 101,
                  "internal_job_id": 9001,
                  "title": "Junior Software Engineer",
                  "company_name": "Acme Corp",
                  "updated_at": "2026-09-10T09:00:00-04:00",
                  "first_published": "2026-09-01T12:00:00-04:00",
                  "requisition_id": "R-1",
                  "location": { "name": "Remote - Brazil" },
                  "absolute_url": "https://boards.greenhouse.io/acme/jobs/101",
                  "metadata": null,
                  "content": "&lt;p&gt;Requirements:&lt;/p&gt;&lt;ul&gt;&lt;li&gt;Java 17 and Spring Boot&lt;/li&gt;&lt;/ul&gt;",
                  "departments": [ { "id": 1, "name": "Engineering", "parent_id": null, "child_ids": [] } ],
                  "offices": [ { "id": 2, "name": "Remote", "location": "Remote", "parent_id": null, "child_ids": [] } ]
                },
                {
                  "id": 102,
                  "title": "Software Engineering Intern",
                  "updated_at": "2026-09-11T09:00:00-04:00",
                  "location": { "name": "Remote" },
                  "absolute_url": "https://boards.greenhouse.io/acme/jobs/102",
                  "content": "&lt;p&gt;Java ou Kotlin&lt;/p&gt;"
                },
                {
                  "id": 103,
                  "title": "Senior Java Engineer",
                  "location": { "name": "Remote" },
                  "absolute_url": "https://boards.greenhouse.io/acme/jobs/103",
                  "content": "&lt;p&gt;Java&lt;/p&gt;"
                },
                {
                  "id": 104,
                  "title": "Junior Frontend Engineer",
                  "location": { "name": "Remote" },
                  "absolute_url": "https://boards.greenhouse.io/acme/jobs/104",
                  "content": "&lt;p&gt;JavaScript, React e CSS&lt;/p&gt;"
                },
                {
                  "id": 105,
                  "title": "Junior Java Developer",
                  "location": { "name": "New York, NY" },
                  "absolute_url": "https://boards.greenhouse.io/acme/jobs/105",
                  "content": "&lt;p&gt;Java&lt;/p&gt;"
                },
                {
                  "id": 106,
                  "title": "Junior Account Executive",
                  "location": { "name": "Remote" },
                  "absolute_url": "https://boards.greenhouse.io/acme/jobs/106",
                  "content": "&lt;p&gt;Experience with Java is a plus&lt;/p&gt;"
                }
              ],
              "meta": { "total": 6 }
            }
            """;

    private final GreenhouseApiClient apiClient = mock(GreenhouseApiClient.class);
    private final RobotsTxtChecker robotsTxtChecker = mock(RobotsTxtChecker.class);
    private final GreenhouseCrawlerProperties properties = new GreenhouseCrawlerProperties();
    private final GreenhouseCrawler crawler = new GreenhouseCrawler(apiClient, properties, robotsTxtChecker);

    @Test
    void coletar_semEmpresasConfiguradas_naoDeveFazerNada() {
        assertThat(crawler.coletar()).isEmpty();

        verifyNoInteractions(apiClient, robotsTxtChecker);
    }

    @Test
    void coletar_deveManterSoEstagioEJuniorDeTecnologiaRemotosComJava() {
        prepararEmpresaComFixture();

        List<Vaga> vagas = crawler.coletar();

        assertThat(vagas).extracting(Vaga::getTitulo)
                .containsExactly("Junior Software Engineer", "Software Engineering Intern");
        assertThat(vagas).extracting(Vaga::getNivel).containsExactly(NivelVaga.JUNIOR, NivelVaga.ESTAGIO);
    }

    @Test
    void coletar_deveMapearOsCamposDaVaga() {
        prepararEmpresaComFixture();

        List<Vaga> vagas = crawler.coletar();

        Vaga comNome = vagas.get(0);
        assertThat(comNome.getEmpresa()).isEqualTo("Acme Corp");
        assertThat(comNome.getLocal()).isEqualTo("Remoto (Remote - Brazil)");
        assertThat(comNome.getLink()).isEqualTo("https://boards.greenhouse.io/acme/jobs/101");
        assertThat(comNome.getFonte()).isEqualTo("GREENHOUSE");
        assertThat(comNome.getDataPublicacao()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(comNome.isRemoto()).isTrue();
        assertThat(comNome.isInternacional()).isFalse();

        Vaga semNome = vagas.get(1);
        assertThat(semNome.getEmpresa()).isEqualTo("acme");
        assertThat(semNome.getLocal()).isEqualTo("Remoto");
        assertThat(semNome.getDataPublicacao()).isEqualTo(LocalDate.of(2026, 9, 11));
    }

    @Test
    void coletar_robotsProibindo_naoDeveBuscarVagas() {
        properties.setEmpresas(List.of("acme"));
        when(robotsTxtChecker.isPermitido(anyString(), anyString())).thenReturn(false);

        assertThat(crawler.coletar()).isEmpty();

        verifyNoInteractions(apiClient);
    }

    @Test
    void coletar_falhaNumaEmpresa_naoDeveImpedirAsOutras() {
        properties.setEmpresas(List.of("quebrada", "acme"));
        when(robotsTxtChecker.isPermitido(anyString(), anyString())).thenReturn(true);
        when(apiClient.buscarVagas("quebrada")).thenThrow(new IllegalStateException("resposta inesperada"));
        when(apiClient.buscarVagas("acme")).thenReturn(lerFixture().jobs());

        assertThat(crawler.coletar()).hasSize(2);
    }

    private void prepararEmpresaComFixture() {
        properties.setEmpresas(List.of("acme"));
        when(robotsTxtChecker.isPermitido(anyString(), anyString())).thenReturn(true);
        when(apiClient.buscarVagas("acme")).thenReturn(lerFixture().jobs());
    }

    private static GreenhouseJobsDto lerFixture() {
        return JsonMapper.builder().build().readValue(FIXTURE, GreenhouseJobsDto.class);
    }
}
