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

/**
 * Roda todos os crawlers cadastrados, isolando falhas: se um crawler quebrar
 * (site fora do ar, mudança de HTML/API), o erro é logado e os demais crawlers
 * continuam normalmente.
 */
@Service
public class ColetaService {

    private static final Logger log = LoggerFactory.getLogger(ColetaService.class);

    private final List<VagaCrawler> crawlers;
    private final DeduplicacaoService deduplicacaoService;
    private final VagaRepository vagaRepository;

    public ColetaService(List<VagaCrawler> crawlers, DeduplicacaoService deduplicacaoService, VagaRepository vagaRepository) {
        this.crawlers = crawlers;
        this.deduplicacaoService = deduplicacaoService;
        this.vagaRepository = vagaRepository;
    }

    public Map<String, Integer> executarTodos() {
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
