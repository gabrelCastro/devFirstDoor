package com.devfirstdoor.service;

import com.devfirstdoor.controller.dto.TesteBoardRequest;
import com.devfirstdoor.controller.dto.TesteBoardResponse;
import com.devfirstdoor.controller.dto.TesteBoardResponse.MotivoReprovacao;
import com.devfirstdoor.crawler.ConsultaBoard;
import com.devfirstdoor.crawler.greenhouse.GreenhouseApiClient;
import com.devfirstdoor.crawler.greenhouse.GreenhouseCrawlerProperties;
import com.devfirstdoor.crawler.greenhouse.dto.GreenhouseJobDto;
import com.devfirstdoor.crawler.greenhouse.dto.GreenhouseLocationDto;
import com.devfirstdoor.crawler.lever.LeverApiClient;
import com.devfirstdoor.crawler.lever.LeverCrawlerProperties;
import com.devfirstdoor.crawler.lever.dto.LeverCategoriesDto;
import com.devfirstdoor.crawler.lever.dto.LeverPostingDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TesteBoardServiceTest {

    private final GreenhouseApiClient greenhouseApiClient = mock(GreenhouseApiClient.class);
    private final LeverApiClient leverApiClient = mock(LeverApiClient.class);
    private final TesteBoardService service = new TesteBoardService(
            greenhouseApiClient,
            leverApiClient,
            new GreenhouseCrawlerProperties(),
            new LeverCrawlerProperties()
    );

    @Test
    void testarGreenhouse_deveContarVagasEExplicarCadaFiltroReprovado() {
        List<GreenhouseJobDto> vagas = List.of(
                greenhouse(1, "Junior Software Engineer", "Remote - Brazil", "Java e Spring"),
                greenhouse(2, "Senior Java Engineer", "Remote", "Java"),
                greenhouse(3, "Junior Account Executive", "Remote", "Java"),
                greenhouse(4, "Junior Frontend Engineer", "Remote", "JavaScript e React"),
                greenhouse(5, "Junior Java Developer", "New York", "Java")
        );
        when(greenhouseApiClient.buscarBoard("acme")).thenReturn(ConsultaBoard.existente(vagas));

        TesteBoardResponse resposta = service.testar(
                new TesteBoardRequest(TesteBoardRequest.Ats.GREENHOUSE, " acme "));

        assertThat(resposta.existe()).isTrue();
        assertThat(resposta.empresa()).isEqualTo("acme");
        assertThat(resposta.totalVagas()).isEqualTo(5);
        assertThat(resposta.totalAprovadas()).isEqualTo(1);
        assertThat(resposta.exemplosAprovados()).extracting(TesteBoardResponse.ExemploAprovado::titulo)
                .containsExactly("Junior Software Engineer");
        assertThat(resposta.exemplosReprovados()).extracting(TesteBoardResponse.ExemploReprovado::motivo)
                .containsExactly(
                        MotivoReprovacao.NIVEL,
                        MotivoReprovacao.FORA_DE_TECNOLOGIA,
                        MotivoReprovacao.NAO_JAVA,
                        MotivoReprovacao.NAO_REMOTA
                );
    }

    @Test
    void testarLever_deveUsarModalidadeEDescricaoDoClassificadorExistente() {
        List<LeverPostingDto> vagas = List.of(
                lever("1", "Backend Engineering Intern", "remote", "São Paulo", "Java ou Kotlin"),
                lever("2", "Junior Java Developer", "hybrid", "Remote", "Java")
        );
        when(leverApiClient.buscarBoard("globex")).thenReturn(ConsultaBoard.existente(vagas));

        TesteBoardResponse resposta = service.testar(
                new TesteBoardRequest(TesteBoardRequest.Ats.LEVER, "globex"));

        assertThat(resposta.totalVagas()).isEqualTo(2);
        assertThat(resposta.totalAprovadas()).isEqualTo(1);
        assertThat(resposta.exemplosReprovados()).singleElement()
                .extracting(TesteBoardResponse.ExemploReprovado::motivo)
                .isEqualTo(MotivoReprovacao.NAO_REMOTA);
    }

    @Test
    void testar_deveDistinguirBoardInexistenteELimitarAsAmostras() {
        when(leverApiClient.buscarBoard("nao-existe")).thenReturn(ConsultaBoard.inexistente());

        TesteBoardResponse inexistente = service.testar(
                new TesteBoardRequest(TesteBoardRequest.Ats.LEVER, "nao-existe"));

        assertThat(inexistente.existe()).isFalse();
        assertThat(inexistente.totalVagas()).isZero();
        assertThat(inexistente.exemplosAprovados()).isEmpty();
        assertThat(inexistente.exemplosReprovados()).isEmpty();

        List<GreenhouseJobDto> vagas = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            vagas.add(greenhouse((long) i, "Junior Software Engineer", "Remote", "Java"));
            vagas.add(greenhouse((long) i + 100, "Senior Software Engineer", "Remote", "Java"));
        }
        when(greenhouseApiClient.buscarBoard("grande")).thenReturn(ConsultaBoard.existente(vagas));

        TesteBoardResponse grande = service.testar(
                new TesteBoardRequest(TesteBoardRequest.Ats.GREENHOUSE, "grande"));

        assertThat(grande.totalVagas()).isEqualTo(24);
        assertThat(grande.totalAprovadas()).isEqualTo(12);
        assertThat(grande.exemplosAprovados()).hasSize(10);
        assertThat(grande.exemplosReprovados()).hasSize(10);
    }

    private static GreenhouseJobDto greenhouse(long id, String titulo, String local, String conteudo) {
        return new GreenhouseJobDto(
                id,
                titulo,
                "Acme",
                new GreenhouseLocationDto(local),
                "https://boards.greenhouse.io/acme/jobs/" + id,
                conteudo,
                null,
                null
        );
    }

    private static LeverPostingDto lever(String id, String titulo, String modalidade, String local,
                                          String descricao) {
        return new LeverPostingDto(
                id,
                titulo,
                new LeverCategoriesDto(local, null, null),
                modalidade,
                null,
                descricao,
                List.of(),
                null,
                "https://jobs.lever.co/globex/" + id
        );
    }
}
