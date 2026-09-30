package com.devfirstdoor.service;

import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.VagaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ColetaServiceTest {

    private final VagaRepository vagaRepository = mock(VagaRepository.class);
    private final DeduplicacaoService deduplicacaoService = new DeduplicacaoService(vagaRepository);
    private final ExpiracaoVagasService expiracaoVagasService = new ExpiracaoVagasService(vagaRepository, 7);

    @Test
    void executarTodos_deveIgnorarNovaColetaEnquantoOutraEstaRodando() throws Exception {
        CrawlerTravado crawler = new CrawlerTravado();
        ColetaService coletaService = new ColetaService(List.of(crawler), deduplicacaoService, vagaRepository, expiracaoVagasService);

        CompletableFuture<Map<String, Integer>> primeira = CompletableFuture.supplyAsync(coletaService::executarTodos);
        assertThat(crawler.iniciou.await(5, TimeUnit.SECONDS)).isTrue();

        Map<String, Integer> segunda = coletaService.executarTodos();

        crawler.liberar.countDown();
        assertThat(segunda).isEmpty();
        assertThat(primeira.get(5, TimeUnit.SECONDS)).containsEntry("FALSO", 0);
        assertThat(crawler.execucoes.get()).isEqualTo(1);
    }

    @Test
    void executarTodos_deveLiberarATravaAoTerminar_mesmoComCrawlerFalhando() {
        AtomicInteger execucoes = new AtomicInteger();
        VagaCrawler quebrado = new VagaCrawler() {
            @Override
            public String getFonte() {
                return "QUEBRADO";
            }

            @Override
            public List<Vaga> coletar() {
                execucoes.incrementAndGet();
                throw new IllegalStateException("fonte fora do ar");
            }
        };
        ColetaService coletaService = new ColetaService(List.of(quebrado), deduplicacaoService, vagaRepository, expiracaoVagasService);

        assertThat(coletaService.executarTodos()).containsEntry("QUEBRADO", -1);
        assertThat(coletaService.executarTodos()).containsEntry("QUEBRADO", -1);
        assertThat(execucoes.get()).isEqualTo(2);
    }

    @Test
    void executarTodos_crawlerComExcecao_naoDeveImpedirOsOutrosDeSalvar() {
        Vaga vaga = new Vaga("Desenvolvedor Java Júnior", "Empresa", "Remoto", NivelVaga.JUNIOR,
                "https://exemplo.com/vaga/1", "BOM", null, LocalDateTime.now());
        VagaCrawler quebrado = crawlerFalso("QUEBRADO", () -> {
            throw new IllegalStateException("HTML mudou");
        });
        VagaCrawler bom = crawlerFalso("BOM", () -> List.of(vaga));
        ColetaService coletaService = new ColetaService(List.of(quebrado, bom), deduplicacaoService, vagaRepository, expiracaoVagasService);

        Map<String, Integer> resultado = coletaService.executarTodos();

        assertThat(resultado).containsEntry("QUEBRADO", -1).containsEntry("BOM", 1);
        verify(vagaRepository).saveAll(List.of(vaga));
    }

    private static VagaCrawler crawlerFalso(String fonte, Supplier<List<Vaga>> coleta) {
        return new VagaCrawler() {
            @Override
            public String getFonte() {
                return fonte;
            }

            @Override
            public List<Vaga> coletar() {
                return coleta.get();
            }
        };
    }

    /**
     * Crawler que fica parado dentro de coletar() até o teste liberar, simulando uma
     * coleta demorada em andamento.
     */
    private static class CrawlerTravado implements VagaCrawler {

        final CountDownLatch iniciou = new CountDownLatch(1);
        final CountDownLatch liberar = new CountDownLatch(1);
        final AtomicInteger execucoes = new AtomicInteger();

        @Override
        public String getFonte() {
            return "FALSO";
        }

        @Override
        public List<Vaga> coletar() {
            execucoes.incrementAndGet();
            iniciou.countDown();
            try {
                liberar.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        }
    }
}
