package com.devfirstdoor.crawler.lever;

import com.devfirstdoor.crawler.lever.dto.LeverPostingDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.robots.RobotsTxtChecker;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * O client e o RobotsTxtChecker são mocks: nenhum teste faz requisição ao Lever.
 * O JSON segue o formato documentado de GET /v0/postings/{empresa}?mode=json.
 */
class LeverCrawlerTest {

    private static final String FIXTURE = """
            [
              {
                "id": "a1b2c3",
                "text": "Desenvolvedor Java Júnior",
                "categories": { "commitment": "Full-time", "department": "Engineering", "location": "Remote - Brazil", "team": "Backend", "allLocations": ["Remote - Brazil"] },
                "country": "BR",
                "workplaceType": "remote",
                "createdAt": 1788264000000,
                "description": "<div>Time de plataforma</div>",
                "descriptionPlain": "Time de plataforma",
                "lists": [ { "text": "Requisitos", "content": "<li>Spring Boot</li>" } ],
                "additional": "",
                "additionalPlain": "",
                "hostedUrl": "https://jobs.lever.co/globex/a1b2c3",
                "applyUrl": "https://jobs.lever.co/globex/a1b2c3/apply"
              },
              {
                "id": "d4e5f6",
                "text": "Backend Engineering Intern",
                "categories": { "commitment": "Internship", "location": "São Paulo" },
                "workplaceType": "remote",
                "createdAt": 1788264000000,
                "descriptionPlain": "Our backend runs on the JVM.",
                "lists": [ { "text": "Requirements", "content": "<li>Java or Kotlin</li><li>SQL</li>" } ],
                "hostedUrl": "https://jobs.lever.co/globex/d4e5f6"
              },
              {
                "id": "g7h8i9",
                "text": "Junior Backend Developer",
                "categories": { "location": "Remote" },
                "workplaceType": "unspecified",
                "descriptionPlain": "We use Java and PostgreSQL.",
                "hostedUrl": "https://jobs.lever.co/globex/g7h8i9"
              },
              {
                "id": "j1k2l3",
                "text": "Junior Java Developer",
                "categories": { "location": "Remote" },
                "workplaceType": "hybrid",
                "descriptionPlain": "Java",
                "hostedUrl": "https://jobs.lever.co/globex/j1k2l3"
              },
              {
                "id": "m4n5o6",
                "text": "Junior Frontend Developer",
                "categories": { "location": "Remote" },
                "workplaceType": "remote",
                "descriptionPlain": "JavaScript and React",
                "hostedUrl": "https://jobs.lever.co/globex/m4n5o6"
              },
              {
                "id": "p7q8r9",
                "text": "Java Developer",
                "categories": { "location": "Remote" },
                "workplaceType": "remote",
                "descriptionPlain": "Java",
                "hostedUrl": "https://jobs.lever.co/globex/p7q8r9"
              }
            ]
            """;

    private final LeverApiClient apiClient = mock(LeverApiClient.class);
    private final RobotsTxtChecker robotsTxtChecker = mock(RobotsTxtChecker.class);
    private final LeverCrawlerProperties properties = new LeverCrawlerProperties();
    private final LeverCrawler crawler = new LeverCrawler(apiClient, properties, robotsTxtChecker);

    @Test
    void coletar_semEmpresasConfiguradas_naoDeveFazerNada() {
        assertThat(crawler.coletar()).isEmpty();

        verifyNoInteractions(apiClient, robotsTxtChecker);
    }

    @Test
    void coletar_deveManterSoEstagioEJuniorDeTecnologiaRemotosComJava() {
        prepararEmpresaComFixture();

        List<Vaga> vagas = crawler.coletar();

        // j1k2l3 é "hybrid" (a modalidade vale mais que "Remote" no local), m4n5o6 não é
        // Java e p7q8r9 não é estágio nem júnior.
        assertThat(vagas).extracting(Vaga::getTitulo).containsExactly(
                "Desenvolvedor Java Júnior", "Backend Engineering Intern", "Junior Backend Developer");
        assertThat(vagas).extracting(Vaga::getNivel)
                .containsExactly(NivelVaga.JUNIOR, NivelVaga.ESTAGIO, NivelVaga.JUNIOR);
    }

    @Test
    void coletar_deveMapearOsCamposDaVaga() {
        prepararEmpresaComFixture();

        List<Vaga> vagas = crawler.coletar();

        Vaga vaga = vagas.get(0);
        assertThat(vaga.getEmpresa()).isEqualTo("globex");
        assertThat(vaga.getLocal()).isEqualTo("Remoto (Remote - Brazil)");
        assertThat(vaga.getLink()).isEqualTo("https://jobs.lever.co/globex/a1b2c3");
        assertThat(vaga.getFonte()).isEqualTo("LEVER");
        assertThat(vaga.getDataPublicacao()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(vaga.isRemoto()).isTrue();

        assertThat(vagas.get(2).getLocal()).isEqualTo("Remoto");
        assertThat(vagas.get(2).getDataPublicacao()).isNull();
    }

    @Test
    void coletar_robotsProibindo_naoDeveBuscarVagas() {
        properties.setEmpresas(List.of("globex"));
        when(robotsTxtChecker.isPermitido(anyString(), anyString())).thenReturn(false);

        assertThat(crawler.coletar()).isEmpty();

        verifyNoInteractions(apiClient);
    }

    private void prepararEmpresaComFixture() {
        properties.setEmpresas(List.of("globex"));
        when(robotsTxtChecker.isPermitido(anyString(), anyString())).thenReturn(true);
        when(apiClient.buscarVagas("globex")).thenReturn(lerFixture());
    }

    private static List<LeverPostingDto> lerFixture() {
        return JsonMapper.builder().build().readValue(FIXTURE, new TypeReference<List<LeverPostingDto>>() {
        });
    }
}
