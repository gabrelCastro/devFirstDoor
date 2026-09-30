package com.devfirstdoor.service;

import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.VagaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Roda todos os crawlers cadastrados, isolando falhas: se um crawler quebrar
 * (site fora do ar, mudança de HTML/API), o erro é logado e os demais crawlers
 * continuam normalmente.
 *
 * Só uma coleta roda por vez: a inicial, a agendada e a manual (POST /api/vagas/coletar)
 * podem coincidir, e duas juntas dobrariam as requisições às fontes e poderiam salvar
 * a mesma vaga duas vezes (a deduplicação só enxerga o que já está no banco).
 */
@Service
public class ColetaService {

    private static final Logger log = LoggerFactory.getLogger(ColetaService.class);

    private final List<VagaCrawler> crawlers;
    private final DeduplicacaoService deduplicacaoService;
    private final VagaRepository vagaRepository;
    private final AtomicBoolean emExecucao = new AtomicBoolean(false);

    public ColetaService(List<VagaCrawler> crawlers, DeduplicacaoService deduplicacaoService, VagaRepository vagaRepository) {
        this.crawlers = crawlers;
        this.deduplicacaoService = deduplicacaoService;
        this.vagaRepository = vagaRepository;
    }

    /**
     * Roda todos os crawlers. Se já houver uma coleta em andamento, esta é ignorada
     * e devolve um mapa vazio.
     */
    public Map<String, Integer> executarTodos() {
        if (!emExecucao.compareAndSet(false, true)) {
            log.warn("Já existe uma coleta em andamento; esta será ignorada");
            return Map.of();
        }
        try {
            return executarCrawlers();
        } finally {
            emExecucao.set(false);
        }
    }

    private Map<String, Integer> executarCrawlers() {
        Map<String, Integer> vagasNovasPorFonte = new LinkedHashMap<>();

        for (VagaCrawler crawler : crawlers) {
            String fonte = crawler.getFonte();
            try {
                List<Vaga> coletadas = crawler.coletar();
                List<Vaga> novas = deduplicacaoService.filtrarNovas(coletadas);
                vagaRepository.saveAll(novas);
                vagasNovasPorFonte.put(fonte, novas.size());
                log.info("Crawler {} concluído: {} coletada(s), {} nova(s) persistida(s)", fonte, coletadas.size(), novas.size());
            } catch (Exception e) {
                log.error("Crawler {} falhou e será ignorado nesta execução: {}", fonte, e.getMessage(), e);
                vagasNovasPorFonte.put(fonte, -1);
            }
        }
        return vagasNovasPorFonte;
    }
}
