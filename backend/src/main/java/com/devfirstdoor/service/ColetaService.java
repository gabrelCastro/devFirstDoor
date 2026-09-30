package com.devfirstdoor.service;

import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.domain.ExecucaoColeta;
import com.devfirstdoor.domain.ExecucaoFonte;
import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.VagaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Roda todos os crawlers cadastrados, isolando falhas: se um crawler quebrar
 * (site fora do ar, mudança de HTML/API), o erro é logado e os demais crawlers
 * continuam normalmente. Cada execução e o resultado de cada fonte ficam gravados
 * no histórico (ver HistoricoColetaService).
 *
 * Só uma coleta roda por vez: a inicial, a agendada e a manual (POST /api/admin/coletas)
 * podem coincidir, e duas juntas dobrariam as requisições às fontes e poderiam salvar
 * a mesma vaga duas vezes (a deduplicação só enxerga o que já está no banco).
 */
@Service
public class ColetaService {

    private static final Logger log = LoggerFactory.getLogger(ColetaService.class);

    private final List<VagaCrawler> crawlers;
    private final DeduplicacaoService deduplicacaoService;
    private final VagaRepository vagaRepository;
    private final ExpiracaoVagasService expiracaoVagasService;
    private final HistoricoColetaService historicoColetaService;
    private final AtomicBoolean emExecucao = new AtomicBoolean(false);

    public ColetaService(List<VagaCrawler> crawlers, DeduplicacaoService deduplicacaoService,
                         VagaRepository vagaRepository, ExpiracaoVagasService expiracaoVagasService,
                         HistoricoColetaService historicoColetaService) {
        this.crawlers = crawlers;
        this.deduplicacaoService = deduplicacaoService;
        this.vagaRepository = vagaRepository;
        this.expiracaoVagasService = expiracaoVagasService;
        this.historicoColetaService = historicoColetaService;
    }

    /**
     * Roda todos os crawlers. Se já houver uma coleta em andamento, esta é ignorada
     * (sem entrar no histórico) e devolve um mapa vazio.
     */
    public Map<String, Integer> executarTodos(OrigemColeta origem) {
        if (!emExecucao.compareAndSet(false, true)) {
            log.warn("Já existe uma coleta em andamento; esta ({}) será ignorada", origem);
            return Map.of();
        }
        try {
            return executarCrawlers(origem);
        } finally {
            emExecucao.set(false);
        }
    }

    private Map<String, Integer> executarCrawlers(OrigemColeta origem) {
        Map<String, Integer> vagasNovasPorFonte = new LinkedHashMap<>();
        ExecucaoColeta execucao = historicoColetaService.iniciar(origem);
        List<ExecucaoFonte> resultados = new ArrayList<>();

        for (VagaCrawler crawler : crawlers) {
            String fonte = crawler.getFonte();
            LocalDateTime inicio = LocalDateTime.now();
            ExecucaoFonte resultado;
            try {
                List<Vaga> coletadas = crawler.coletar();
                List<Vaga> novas = deduplicacaoService.filtrarNovas(coletadas);
                vagaRepository.saveAll(novas);
                deduplicacaoService.registrarVisita(hashes(coletadas), inicio);
                int expiradas = removerExpiradas(fonte, coletadas, inicio);
                vagasNovasPorFonte.put(fonte, novas.size());
                resultado = ExecucaoFonte.sucesso(execucao, fonte, inicio,
                        crawler.contarEncontradas(coletadas), novas.size(), expiradas);
                log.info("Crawler {} concluído: {} coletada(s), {} nova(s) persistida(s), {} expirada(s) removida(s)",
                        fonte, coletadas.size(), novas.size(), expiradas);
            } catch (Exception e) {
                log.error("Crawler {} falhou e será ignorado nesta execução: {}", fonte, e.getMessage(), e);
                vagasNovasPorFonte.put(fonte, -1);
                resultado = ExecucaoFonte.erro(execucao, fonte, inicio, descrever(e));
            }
            historicoColetaService.registrar(resultado);
            resultados.add(resultado);
        }
        historicoColetaService.finalizar(execucao, resultados);
        return vagasNovasPorFonte;
    }

    /** Exceções sem mensagem (ex: NullPointerException) ficam ao menos com o tipo. */
    private static String descrever(Exception e) {
        return e.getMessage() != null ? e.getClass().getSimpleName() + ": " + e.getMessage()
                : e.getClass().getSimpleName();
    }

    /**
     * Os crawlers engolem falhas de rede/robots.txt e devolvem lista vazia, então uma
     * coleta sem nenhuma vaga não prova que a fonte funcionou: nesse caso nada expira.
     */
    private int removerExpiradas(String fonte, List<Vaga> coletadas, LocalDateTime agora) {
        if (coletadas.isEmpty()) {
            return 0;
        }
        return expiracaoVagasService.removerExpiradas(fonte, agora);
    }

    private List<String> hashes(List<Vaga> vagas) {
        return vagas.stream()
                .map(v -> deduplicacaoService.calcularHash(v.getTitulo(), v.getEmpresa(), v.getFonte()))
                .toList();
    }
}
