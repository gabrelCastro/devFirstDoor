package com.devfirstdoor.service;

import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.VagaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
