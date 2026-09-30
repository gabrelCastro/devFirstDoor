package com.devfirstdoor.crawler.gupy;

import com.devfirstdoor.crawler.gupy.dto.GupyJobDto;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.robots.RobotsTxtChecker;
import com.devfirstdoor.service.ConfiguracaoColeta;
import com.devfirstdoor.service.ConfiguracaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O client e o RobotsTxtChecker são mocks: nenhum teste faz requisição à Gupy.
 */
class GupyCrawlerTest {

    private static final String TERMO = "desenvolvedor junior";

    private final GupyApiClient apiClient = mock(GupyApiClient.class);
    private final RobotsTxtChecker robotsTxtChecker = mock(RobotsTxtChecker.class);
    private final GupyCrawlerProperties properties = new GupyCrawlerProperties();
    private final GupyCrawler crawler = new GupyCrawler(apiClient, properties, robotsTxtChecker);

    @BeforeEach
    void setUp() {
        properties.setTermosBusca(List.of(TERMO));
        when(robotsTxtChecker.isPermitido(anyString(), anyString())).thenReturn(true);
    }

    @Test
    void coletar_tituloComJava_naoDeveLerAPaginaDaVaga() {
        when(apiClient.buscarTodasAsPaginas(TERMO)).thenReturn(List.of(job(1L, "Desenvolvedor Java Júnior")));

        List<Vaga> vagas = crawler.coletar();

        assertThat(vagas).extracting(Vaga::getTitulo).containsExactly("Desenvolvedor Java Júnior");
        verify(apiClient, never()).buscarTextoDaVaga(anyString());
    }

    @Test
    void coletar_tituloSemJava_deveLerAPaginaUmaVezSo() {
        GupyJobDto job = job(2L, "Desenvolvedor Back-end Júnior");
        when(apiClient.buscarTodasAsPaginas(TERMO)).thenReturn(List.of(job));
        when(apiClient.buscarTextoDaVaga(job.jobUrl())).thenReturn("Requisitos: Java 17, Spring Boot e SQL.");

        assertThat(crawler.coletar()).extracting(Vaga::getTitulo).containsExactly("Desenvolvedor Back-end Júnior");
        assertThat(crawler.coletar()).extracting(Vaga::getTitulo).containsExactly("Desenvolvedor Back-end Júnior");

        verify(apiClient, times(1)).buscarTextoDaVaga(job.jobUrl());
    }

    @Test
    void coletar_tituloSemJava_paginaSemJava_deveDescartarENaoRelerNaColetaSeguinte() {
        GupyJobDto job = job(3L, "Desenvolvedor Front-end Júnior");
        when(apiClient.buscarTodasAsPaginas(TERMO)).thenReturn(List.of(job));
        when(apiClient.buscarTextoDaVaga(job.jobUrl())).thenReturn("Requisitos: JavaScript, React e CSS.");

        assertThat(crawler.coletar()).isEmpty();
        assertThat(crawler.coletar()).isEmpty();

        verify(apiClient, times(1)).buscarTextoDaVaga(job.jobUrl());
    }

    @Test
    void coletar_falhaNaLeituraDaPagina_naoDeveEntrarNoCache() {
        GupyJobDto job = job(4L, "Desenvolvedor Back-end Júnior");
        when(apiClient.buscarTodasAsPaginas(TERMO)).thenReturn(List.of(job));
        when(apiClient.buscarTextoDaVaga(job.jobUrl())).thenReturn(null, "Requisitos: Java e Spring.");

        assertThat(crawler.coletar()).isEmpty();
        assertThat(crawler.coletar()).extracting(Vaga::getTitulo).containsExactly("Desenvolvedor Back-end Júnior");

        verify(apiClient, times(2)).buscarTextoDaVaga(job.jobUrl());
    }

    @Test
    void coletar_deveInformarOProgressoPorTermo() {
        properties.setTermosBusca(List.of(TERMO, "estagio java"));
        when(apiClient.buscarTodasAsPaginas(anyString())).thenReturn(List.of());
        List<String> progresso = new ArrayList<>();

        crawler.coletar(progresso::add);

        assertThat(progresso).containsExactly("termo 1/2", "termo 2/2");
    }

    @Test
    void coletar_deveRelerOsTermosDaConfiguracaoEmCadaColeta() {
        ConfiguracaoService configuracaoService = mock(ConfiguracaoService.class);
        when(configuracaoService.obter()).thenReturn(
                configuracao(List.of("primeiro")),
                configuracao(List.of("segundo"))
        );
        when(apiClient.buscarTodasAsPaginas(anyString())).thenReturn(List.of());
        GupyCrawler crawlerDinamico = new GupyCrawler(
                apiClient, properties, robotsTxtChecker, configuracaoService);

        crawlerDinamico.coletar();
        crawlerDinamico.coletar();

        verify(apiClient).buscarTodasAsPaginas("primeiro");
        verify(apiClient).buscarTodasAsPaginas("segundo");
    }

    private static ConfiguracaoColeta configuracao(List<String> termos) {
        return new ConfiguracaoColeta(
                java.util.Map.of("GUPY", true), termos, List.of(), List.of(), List.of(),
                Duration.ofHours(6), 7, 3000, 2000, false, false);
    }

    private static GupyJobDto job(Long id, String titulo) {
        return new GupyJobDto(id, titulo, "Empresa " + id, "São Paulo", "SP", true,
                "vacancy_type_effective", "2026-09-01T12:00:00.000Z", "https://empresa.gupy.io/jobs/" + id);
    }
}
