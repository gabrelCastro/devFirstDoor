package com.devfirstdoor.crawler.gupy;

import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.crawler.gupy.dto.GupyJobDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.robots.RobotsTxtChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@EnableConfigurationProperties(GupyCrawlerProperties.class)
public class GupyCrawler implements VagaCrawler {

    private static final Logger log = LoggerFactory.getLogger(GupyCrawler.class);

    private final GupyApiClient apiClient;
    private final GupyCrawlerProperties properties;
    private final GupyVagaClassifier classifier;
    private final GupyJobMapper mapper = new GupyJobMapper();
    private final RobotsTxtChecker robotsTxtChecker;

    public GupyCrawler(GupyApiClient apiClient, GupyCrawlerProperties properties, RobotsTxtChecker robotsTxtChecker) {
        this.apiClient = apiClient;
        this.properties = properties;
        this.classifier = new GupyVagaClassifier(properties);
        this.robotsTxtChecker = robotsTxtChecker;
    }

    @Override
    public String getFonte() {
        return GupyJobMapper.FONTE;
    }

    @Override
    public List<Vaga> coletar() {
        if (!podeColetar()) {
            return List.of();
        }

        List<Vaga> vagas = new ArrayList<>();
        Set<Long> idsVistos = new HashSet<>();

        for (String termo : properties.getTermosBusca()) {
            try {
                List<GupyJobDto> jobs = apiClient.buscarTodasAsPaginas(termo);
                for (GupyJobDto job : jobs) {
                    processarJob(job, idsVistos, vagas);
                }
            } catch (Exception e) {
                log.error("Falha ao processar termo de busca '{}' na Gupy: {}", termo, e.getMessage(), e);
            }
        }

        log.info("GupyCrawler coletou {} vaga(s) de estágio/júnior em tecnologia", vagas.size());
        return vagas;
    }

    private void processarJob(GupyJobDto job, Set<Long> idsVistos, List<Vaga> vagas) {
        if (job.id() == null || !idsVistos.add(job.id())) {
            return;
        }
        Optional<NivelVaga> nivel = classifier.classificarNivel(job);
        if (nivel.isEmpty() || !classifier.isRelevanteParaTech(job)) {
            return;
        }
        if (properties.isApenasRemoto() && !classifier.isRemota(job)) {
            return;
        }
        vagas.add(mapper.paraVaga(job, nivel.get()));
    }

    private boolean podeColetar() {
        URI uri = URI.create(properties.getBaseUrl());
        String baseHost = uri.getScheme() + "://" + uri.getHost();
        boolean permitido = robotsTxtChecker.isPermitido(baseHost, uri.getPath());
        if (!permitido) {
            log.warn("Coleta da Gupy abortada: robots.txt de {} não permite acesso a {}", baseHost, uri.getPath());
        }
        return permitido;
    }
}
