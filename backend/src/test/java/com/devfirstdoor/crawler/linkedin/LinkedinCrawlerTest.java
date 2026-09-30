package com.devfirstdoor.crawler.linkedin;

import com.devfirstdoor.crawler.linkedin.LinkedinHtmlClient.LinkedinBloqueadoException;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.VagaRepository;
import com.devfirstdoor.service.DeduplicacaoService;
import com.devfirstdoor.service.ConfiguracaoColeta;
import com.devfirstdoor.service.ConfiguracaoService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O client é um mock: nenhum teste faz requisição ao LinkedIn.
 */
class LinkedinCrawlerTest {

    private static final String DESCRICAO_JAVA = "Vaga 100% remota. Requisitos: Java e Spring Boot.";

    private final LinkedinHtmlClient client = mock(LinkedinHtmlClient.class);
    private final VagaRepository vagaRepository = mock(VagaRepository.class);
    private final DeduplicacaoService deduplicacaoService = new DeduplicacaoService(vagaRepository);
    private final LinkedinCrawlerProperties properties = new LinkedinCrawlerProperties();

    @Test
    void coletar_bloqueioNoMeioDaBusca_deveInterromperOsTermosRestantesEManterOsJaBuscados() {
        properties.setTermosBusca(List.of("java júnior", "java jr", "estágio java"));
        when(client.buscarTodasAsPaginas("java júnior")).thenReturn(List.of(job("1", "Desenvolvedor Java Júnior")));
        when(client.buscarTodasAsPaginas("java jr")).thenThrow(new LinkedinBloqueadoException(429));
        when(client.buscarDescricao("1")).thenReturn(DESCRICAO_JAVA);

        List<Vaga> vagas = crawler().coletar();

        assertThat(vagas).extracting(Vaga::getTitulo).containsExactly("Desenvolvedor Java Júnior");
        assertThat(vagas.get(0).getLocal()).startsWith("Remoto");
        verify(client, never()).buscarTodasAsPaginas("estágio java");
    }

    @Test
    void coletar_bloqueioNaLeituraDasDescricoes_deveDevolverAsJaLidasEPararDeLer() {
        properties.setTermosBusca(List.of("java júnior"));
        when(client.buscarTodasAsPaginas("java júnior")).thenReturn(List.of(
                job("1", "Desenvolvedor Java Júnior"),
                job("2", "Programador Java Jr"),
                job("3", "Estágio em Desenvolvimento Java")));
        when(client.buscarDescricao("1")).thenReturn(DESCRICAO_JAVA);
        when(client.buscarDescricao("2")).thenThrow(new LinkedinBloqueadoException(999));

        List<Vaga> vagas = crawler().coletar();

        assertThat(vagas).extracting(Vaga::getTitulo).containsExactly("Desenvolvedor Java Júnior");
        verify(client, never()).buscarDescricao("3");
    }

    @Test
    void coletar_vagaSemJava_deveSerDescartadaENaoRelidaNaColetaSeguinte() {
        properties.setTermosBusca(List.of("desenvolvedor júnior"));
        when(client.buscarTodasAsPaginas("desenvolvedor júnior"))
                .thenReturn(List.of(job("7", "Desenvolvedor Júnior")));
        when(client.buscarDescricao("7")).thenReturn("Vaga remota. Requisitos: Python e Django.");
        LinkedinCrawler crawler = crawler();

        assertThat(crawler.coletar()).isEmpty();
        assertThat(crawler.coletar()).isEmpty();

        verify(client, times(2)).buscarTodasAsPaginas("desenvolvedor júnior");
        verify(client, times(1)).buscarDescricao("7");
    }

    @Test
    void coletar_vagaSemDescricaoLida_naoDeveSerMarcadaComoSemJava() {
        properties.setTermosBusca(List.of("desenvolvedor júnior"));
        when(client.buscarTodasAsPaginas("desenvolvedor júnior"))
                .thenReturn(List.of(job("8", "Desenvolvedor Júnior")));
        when(client.buscarDescricao("8")).thenReturn(null, DESCRICAO_JAVA);
        LinkedinCrawler crawler = crawler();

        assertThat(crawler.coletar()).isEmpty();
        assertThat(crawler.coletar()).extracting(Vaga::getTitulo).containsExactly("Desenvolvedor Júnior");
    }

    @Test
    void coletar_vagaJaNoBanco_naoDeveLerADescricaoEDeveRegistrarAVisita() {
        properties.setTermosBusca(List.of("java júnior"));
        LinkedinJobDto jaSalva = job("9", "Desenvolvedor Java Júnior");
        String hash = deduplicacaoService.calcularHash(jaSalva.titulo(), jaSalva.empresa(), LinkedinJobMapper.FONTE);
        when(client.buscarTodasAsPaginas("java júnior")).thenReturn(List.of(jaSalva));
        when(vagaRepository.existsByHashDeduplicacao(hash)).thenReturn(true);

        assertThat(crawler().coletar()).isEmpty();

        verify(client, never()).buscarDescricao(anyString());
        verify(vagaRepository).atualizarDataUltimaVisita(argThat(hashes -> hashes.contains(hash)), any());
    }

    @Test
    void contarEncontradas_deveSomarAsVagasJaSalvasQueNaoSaoDevolvidas() {
        properties.setTermosBusca(List.of("java júnior"));
        LinkedinJobDto jaSalva = job("10", "Desenvolvedor Java Júnior");
        String hash = deduplicacaoService.calcularHash(jaSalva.titulo(), jaSalva.empresa(), LinkedinJobMapper.FONTE);
        when(client.buscarTodasAsPaginas("java júnior")).thenReturn(List.of(jaSalva, job("11", "Programador Java Jr")));
        when(vagaRepository.existsByHashDeduplicacao(hash)).thenReturn(true);
        when(client.buscarDescricao("11")).thenReturn(DESCRICAO_JAVA);
        LinkedinCrawler crawler = crawler();

        List<Vaga> vagas = crawler.coletar();

        assertThat(vagas).extracting(Vaga::getTitulo).containsExactly("Programador Java Jr");
        assertThat(crawler.contarEncontradas(vagas)).isEqualTo(2);
    }

    @Test
    void coletar_semVagasJaSalvas_naoDeveAtualizarVisitas() {
        properties.setTermosBusca(List.of("java júnior"));
        when(client.buscarTodasAsPaginas("java júnior")).thenReturn(List.of());

        assertThat(crawler().coletar()).isEmpty();

        verify(vagaRepository, never()).atualizarDataUltimaVisita(anyCollection(), any());
    }

    @Test
    void coletar_deveInformarOProgressoDosTermosEDasDescricoes() {
        properties.setTermosBusca(List.of("java júnior", "java jr"));
        when(client.buscarTodasAsPaginas("java júnior")).thenReturn(List.of(
                job("1", "Desenvolvedor Java Júnior"),
                job("2", "Programador Java Jr")));
        when(client.buscarTodasAsPaginas("java jr")).thenReturn(List.of());
        when(client.buscarDescricao(anyString())).thenReturn(DESCRICAO_JAVA);
        List<String> progresso = new ArrayList<>();

        crawler().coletar(progresso::add);

        assertThat(progresso).containsExactly("termo 1/2", "termo 2/2", "lendo descrições 1/2", "lendo descrições 2/2");
    }

    @Test
    void coletar_chaveMestraDesligada_naoDeveFazerRequisicao() {
        ConfiguracaoService configuracaoService = mock(ConfiguracaoService.class);
        when(configuracaoService.obter()).thenReturn(new ConfiguracaoColeta(
                java.util.Map.of("LINKEDIN", true), List.of(), List.of("java júnior"), List.of(), List.of(),
                Duration.ofHours(6), 7, 0, 0, false, false));
        LinkedinCrawler crawler = new LinkedinCrawler(client, properties, deduplicacaoService, configuracaoService);

        assertThat(crawler.coletar()).isEmpty();

        verify(client, never()).buscarTodasAsPaginas(anyString());
        verify(client, never()).buscarDescricao(anyString());
    }

    private LinkedinCrawler crawler() {
        return new LinkedinCrawler(client, properties, deduplicacaoService);
    }

    private static LinkedinJobDto job(String id, String titulo) {
        return new LinkedinJobDto(id, titulo, "Empresa " + id, "Brasil",
                "https://www.linkedin.com/jobs/view/" + id, LocalDate.of(2026, 9, 1));
    }
}
