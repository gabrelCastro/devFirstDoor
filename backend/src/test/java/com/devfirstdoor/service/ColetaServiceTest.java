package com.devfirstdoor.service;

import com.devfirstdoor.crawler.ProgressoColeta;
import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.domain.ExecucaoColeta;
import com.devfirstdoor.domain.ExecucaoFonte;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.domain.StatusExecucao;
import com.devfirstdoor.domain.StatusFonte;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.ExecucaoColetaRepository;
import com.devfirstdoor.repository.ExecucaoFonteRepository;
import com.devfirstdoor.repository.VagaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ColetaServiceTest {

    private final VagaRepository vagaRepository = mock(VagaRepository.class);
    private final ExecucaoColetaRepository execucaoRepository = mock(ExecucaoColetaRepository.class);
    private final ExecucaoFonteRepository execucaoFonteRepository = mock(ExecucaoFonteRepository.class);
    private final DeduplicacaoService deduplicacaoService = new DeduplicacaoService(vagaRepository);
    private final ExpiracaoVagasService expiracaoVagasService = new ExpiracaoVagasService(vagaRepository, 7);
    private final HistoricoColetaService historicoColetaService =
            new HistoricoColetaService(execucaoRepository, execucaoFonteRepository);
    private final AndamentoColeta andamento = new AndamentoColeta();

    ColetaServiceTest() {
        when(execucaoRepository.save(any())).then(returnsFirstArg());
    }

    @Test
    void executarTodos_deveIgnorarNovaColetaEnquantoOutraEstaRodando() throws Exception {
        CrawlerTravado crawler = new CrawlerTravado();
        ColetaService coletaService = coletaService(crawler);

        CompletableFuture<Map<String, Integer>> primeira =
                CompletableFuture.supplyAsync(() -> coletaService.executarTodos(OrigemColeta.AGENDADA));
        assertThat(crawler.iniciou.await(5, TimeUnit.SECONDS)).isTrue();

        Map<String, Integer> segunda = coletaService.executarTodos(OrigemColeta.MANUAL);

        crawler.liberar.countDown();
        assertThat(segunda).isEmpty();
        assertThat(primeira.get(5, TimeUnit.SECONDS)).containsEntry("FALSO", 0);
        assertThat(crawler.execucoes.get()).isEqualTo(1);
        assertThat(execucoesGravadas()).extracting(ExecucaoColeta::getOrigem).containsOnly(OrigemColeta.AGENDADA);
    }

    @Test
    void executarTodos_deveLiberarATravaAoTerminar_mesmoComCrawlerFalhando() {
        AtomicInteger execucoes = new AtomicInteger();
        VagaCrawler quebrado = crawlerFalso("QUEBRADO", () -> {
            execucoes.incrementAndGet();
            throw new IllegalStateException("fonte fora do ar");
        });
        ColetaService coletaService = coletaService(quebrado);

        assertThat(coletaService.executarTodos(OrigemColeta.MANUAL)).containsEntry("QUEBRADO", -1);
        assertThat(coletaService.executarTodos(OrigemColeta.MANUAL)).containsEntry("QUEBRADO", -1);
        assertThat(execucoes.get()).isEqualTo(2);
    }

    @Test
    void executarTodos_crawlerComExcecao_naoDeveImpedirOsOutrosDeSalvar() {
        Vaga vaga = vaga("Desenvolvedor Java Júnior");
        VagaCrawler quebrado = crawlerFalso("QUEBRADO", () -> {
            throw new IllegalStateException("HTML mudou");
        });
        VagaCrawler bom = crawlerFalso("BOM", () -> List.of(vaga));
        ColetaService coletaService = coletaService(quebrado, bom);

        Map<String, Integer> resultado = coletaService.executarTodos(OrigemColeta.MANUAL);

        assertThat(resultado).containsEntry("QUEBRADO", -1).containsEntry("BOM", 1);
        verify(vagaRepository).saveAll(List.of(vaga));
    }

    @Test
    void executarTodos_deveGravarAExecucaoComAOrigemEOResultadoDeCadaFonte() {
        VagaCrawler quebrado = crawlerFalso("QUEBRADO", () -> {
            throw new IllegalStateException("HTML mudou");
        });
        VagaCrawler bom = crawlerFalso("BOM", () -> List.of(vaga("Estágio Java"), vaga("Júnior Java")));

        coletaService(quebrado, bom).executarTodos(OrigemColeta.INICIAL);

        ExecucaoColeta execucao = ultimaExecucaoGravada();
        assertThat(execucao.getOrigem()).isEqualTo(OrigemColeta.INICIAL);
        assertThat(execucao.getStatus()).isEqualTo(StatusExecucao.PARCIAL);
        assertThat(execucao.getFim()).isNotNull();

        List<ExecucaoFonte> fontes = fontesGravadas();
        assertThat(fontes).extracting(ExecucaoFonte::getFonte).containsExactly("QUEBRADO", "BOM");
        assertThat(fontes).allSatisfy(f -> assertThat(f.getExecucao()).isSameAs(execucao));

        ExecucaoFonte erro = fontes.get(0);
        assertThat(erro.getStatus()).isEqualTo(StatusFonte.ERRO);
        assertThat(erro.getMensagemErro()).isEqualTo("IllegalStateException: HTML mudou");
        assertThat(erro.getFim()).isAfterOrEqualTo(erro.getInicio());

        ExecucaoFonte sucesso = fontes.get(1);
        assertThat(sucesso.getStatus()).isEqualTo(StatusFonte.SUCESSO);
        assertThat(sucesso.getEncontradas()).isEqualTo(2);
        assertThat(sucesso.getNovas()).isEqualTo(2);
        assertThat(sucesso.getMensagemErro()).isNull();
    }

    @Test
    void executarTodos_encontradasDeveVirDoCrawler() {
        VagaCrawler soNovas = new VagaCrawler() {
            @Override
            public String getFonte() {
                return "SO_NOVAS";
            }

            @Override
            public List<Vaga> coletar() {
                return List.of();
            }

            @Override
            public int contarEncontradas(List<Vaga> devolvidas) {
                return 5;
            }
        };

        coletaService(soNovas).executarTodos(OrigemColeta.AGENDADA);

        ExecucaoFonte resultado = fontesGravadas().get(0);
        assertThat(resultado.getEncontradas()).isEqualTo(5);
        assertThat(resultado.getNovas()).isZero();
        assertThat(ultimaExecucaoGravada().getStatus()).isEqualTo(StatusExecucao.SUCESSO);
    }

    @Test
    void executarTodos_todasAsFontesFalhando_deveGravarErroComMensagemTruncada() {
        VagaCrawler quebrado = crawlerFalso("QUEBRADO", () -> {
            throw new IllegalStateException("x".repeat(5000));
        });

        coletaService(quebrado).executarTodos(OrigemColeta.MANUAL);

        assertThat(ultimaExecucaoGravada().getStatus()).isEqualTo(StatusExecucao.ERRO);
        assertThat(fontesGravadas().get(0).getMensagemErro()).hasSize(ExecucaoFonte.TAMANHO_MAXIMO_MENSAGEM);
    }

    @Test
    void executarTodos_deveExporAFonteAtualEOProgressoEnquantoRoda() {
        List<AndamentoColeta.Estado> vistos = new ArrayList<>();
        VagaCrawler comProgresso = new VagaCrawler() {
            @Override
            public String getFonte() {
                return "COM_PROGRESSO";
            }

            @Override
            public List<Vaga> coletar() {
                return List.of();
            }

            @Override
            public List<Vaga> coletar(ProgressoColeta progresso) {
                vistos.add(andamento.atual().orElseThrow());
                progresso.informar("termo 1/2");
                vistos.add(andamento.atual().orElseThrow());
                return List.of();
            }
        };
        VagaCrawler semProgresso = crawlerFalso("SEM_PROGRESSO", () -> {
            vistos.add(andamento.atual().orElseThrow());
            return List.of();
        });

        coletaService(comProgresso, semProgresso).executarTodos(OrigemColeta.MANUAL);

        assertThat(vistos).extracting(AndamentoColeta.Estado::origem).containsOnly(OrigemColeta.MANUAL);
        assertThat(vistos).extracting(AndamentoColeta.Estado::fonteAtual)
                .containsExactly("COM_PROGRESSO", "COM_PROGRESSO", "SEM_PROGRESSO");
        assertThat(vistos).extracting(AndamentoColeta.Estado::progresso)
                .containsExactly(null, "termo 1/2", null);
        assertThat(andamento.atual()).isEmpty();
    }

    @Test
    void executarTodos_crawlerFalhando_deveLimparOEstadoAoTerminar() {
        VagaCrawler quebrado = crawlerFalso("QUEBRADO", () -> {
            throw new IllegalStateException("fonte fora do ar");
        });

        coletaService(quebrado).executarTodos(OrigemColeta.MANUAL);

        assertThat(andamento.atual()).isEmpty();
    }

    @Test
    void executarTodos_deveLimparDescartesAntigosAoFimDaColeta() {
        DescarteService descarteService = mock(DescarteService.class);
        ColetaService coletaService = new ColetaService(
                List.of(crawlerFalso("BOM", List::of)), deduplicacaoService, vagaRepository,
                expiracaoVagasService, historicoColetaService, andamento, descarteService, Runnable::run);

        coletaService.executarTodos(OrigemColeta.AGENDADA);

        verify(descarteService).removerAntigos();
    }

    @Test
    void disparar_deveRodarEmSegundoPlanoESegurarATravaAteTerminar() {
        AtomicInteger execucoes = new AtomicInteger();
        VagaCrawler bom = crawlerFalso("BOM", () -> {
            execucoes.incrementAndGet();
            return List.of(vaga("Estágio Java"));
        });
        List<Runnable> agendadas = new ArrayList<>();
        ColetaService coletaService = coletaService(agendadas::add, bom);

        assertThat(coletaService.disparar(OrigemColeta.MANUAL)).isEqualTo(ColetaService.Disparo.INICIADA);

        // Ainda não rodou, mas a trava já está pega: nada mais começa até a tarefa terminar
        assertThat(execucoes.get()).isZero();
        assertThat(andamento.atual()).map(AndamentoColeta.Estado::origem).contains(OrigemColeta.MANUAL);
        assertThat(coletaService.disparar(OrigemColeta.MANUAL)).isEqualTo(ColetaService.Disparo.JA_EM_ANDAMENTO);
        assertThat(coletaService.disparar(OrigemColeta.MANUAL, "BOM")).isEqualTo(ColetaService.Disparo.JA_EM_ANDAMENTO);
        assertThat(coletaService.executarTodos(OrigemColeta.AGENDADA)).isEmpty();

        assertThat(agendadas).hasSize(1);
        agendadas.get(0).run();

        assertThat(execucoes.get()).isEqualTo(1);
        assertThat(andamento.atual()).isEmpty();
        assertThat(ultimaExecucaoGravada().getOrigem()).isEqualTo(OrigemColeta.MANUAL);
        assertThat(coletaService.disparar(OrigemColeta.MANUAL)).isEqualTo(ColetaService.Disparo.INICIADA);
    }

    @Test
    void disparar_umaFonte_deveRodarSoEla() {
        AtomicInteger outras = new AtomicInteger();
        VagaCrawler outra = crawlerFalso("OUTRA", () -> {
            outras.incrementAndGet();
            return List.of();
        });
        VagaCrawler alvo = crawlerFalso("ALVO", () -> List.of(vaga("Júnior Java")));
        ColetaService coletaService = coletaService(Runnable::run, outra, alvo);

        assertThat(coletaService.disparar(OrigemColeta.MANUAL, "alvo")).isEqualTo(ColetaService.Disparo.INICIADA);

        assertThat(outras.get()).isZero();
        assertThat(fontesGravadas()).extracting(ExecucaoFonte::getFonte).containsExactly("ALVO");
        assertThat(ultimaExecucaoGravada().getStatus()).isEqualTo(StatusExecucao.SUCESSO);
        assertThat(andamento.atual()).isEmpty();
    }

    @Test
    void disparar_fonteInexistenteOuDesligada_naoDeveRodarNada() {
        AtomicInteger execucoes = new AtomicInteger();
        VagaCrawler desligado = new VagaCrawler() {
            @Override
            public String getFonte() {
                return "DESLIGADO";
            }

            @Override
            public List<Vaga> coletar() {
                execucoes.incrementAndGet();
                return List.of();
            }

            @Override
            public boolean isLigada() {
                return false;
            }
        };
        List<Runnable> agendadas = new ArrayList<>();
        ColetaService coletaService = coletaService(agendadas::add, desligado);

        assertThat(coletaService.disparar(OrigemColeta.MANUAL, "NAO_EXISTE"))
                .isEqualTo(ColetaService.Disparo.FONTE_INEXISTENTE);
        assertThat(coletaService.disparar(OrigemColeta.MANUAL, "DESLIGADO"))
                .isEqualTo(ColetaService.Disparo.FONTE_DESLIGADA);
        // LinkedIn desligado nem vira bean, mas é uma fonte conhecida
        assertThat(coletaService.disparar(OrigemColeta.MANUAL, "LINKEDIN"))
                .isEqualTo(ColetaService.Disparo.FONTE_DESLIGADA);

        assertThat(agendadas).isEmpty();
        assertThat(execucoes.get()).isZero();
        assertThat(andamento.atual()).isEmpty();
    }

    @Test
    void disparar_semConseguirAgendar_deveLiberarATrava() {
        ColetaService coletaService = coletaService(tarefa -> {
            throw new RejectedExecutionException("sem threads");
        }, crawlerFalso("BOM", List::of));

        assertThatThrownBy(() -> coletaService.disparar(OrigemColeta.MANUAL))
                .isInstanceOf(RejectedExecutionException.class);

        assertThat(andamento.atual()).isEmpty();
        assertThat(coletaService.executarTodos(OrigemColeta.MANUAL)).containsEntry("BOM", 0);
    }

    private ColetaService coletaService(VagaCrawler... crawlers) {
        return new ColetaService(List.of(crawlers), deduplicacaoService, vagaRepository, expiracaoVagasService,
                historicoColetaService, andamento);
    }

    private ColetaService coletaService(Executor segundoPlano, VagaCrawler... crawlers) {
        return new ColetaService(List.of(crawlers), deduplicacaoService, vagaRepository, expiracaoVagasService,
                historicoColetaService, andamento, segundoPlano);
    }

    private List<ExecucaoColeta> execucoesGravadas() {
        ArgumentCaptor<ExecucaoColeta> captor = ArgumentCaptor.forClass(ExecucaoColeta.class);
        verify(execucaoRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private ExecucaoColeta ultimaExecucaoGravada() {
        List<ExecucaoColeta> execucoes = execucoesGravadas();
        return execucoes.get(execucoes.size() - 1);
    }

    private List<ExecucaoFonte> fontesGravadas() {
        ArgumentCaptor<ExecucaoFonte> captor = ArgumentCaptor.forClass(ExecucaoFonte.class);
        verify(execucaoFonteRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private static Vaga vaga(String titulo) {
        return new Vaga(titulo, "Empresa", "Remoto", NivelVaga.JUNIOR,
                "https://exemplo.com/vaga/" + titulo.hashCode(), "BOM", null, LocalDateTime.now());
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
