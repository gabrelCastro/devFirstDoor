package com.devfirstdoor.crawler.lever;

import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.crawler.lever.dto.LeverPostingDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.robots.RobotsTxtChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@EnableConfigurationProperties(LeverCrawlerProperties.class)
public class LeverCrawler implements VagaCrawler {

    private static final Logger log = LoggerFactory.getLogger(LeverCrawler.class);
    private static final String CAMINHO_API = "/v0/postings/";

    private final LeverApiClient apiClient;
    private final LeverCrawlerProperties properties;
    private final LeverVagaClassifier classifier;
    private final LeverJobMapper mapper = new LeverJobMapper();
    private final RobotsTxtChecker robotsTxtChecker;

    public LeverCrawler(LeverApiClient apiClient, LeverCrawlerProperties properties, RobotsTxtChecker robotsTxtChecker) {
        this.apiClient = apiClient;
        this.properties = properties;
        this.classifier = new LeverVagaClassifier(properties);
        this.robotsTxtChecker = robotsTxtChecker;
    }

    @Override
    public String getFonte() {
        return LeverJobMapper.FONTE;
    }

    @Override
    public boolean isLigada() {
        return !properties.getEmpresas().isEmpty();
    }

    @Override
    public List<Vaga> coletar() {
        if (!isLigada() || !podeColetar()) {
            return List.of();
        }

        List<Vaga> vagas = new ArrayList<>();
        Set<String> idsVistos = new HashSet<>();

        for (String empresa : properties.getEmpresas()) {
            try {
                for (LeverPostingDto posting : apiClient.buscarVagas(empresa)) {
                    processarPosting(posting, empresa, idsVistos, vagas);
                }
            } catch (Exception e) {
                log.error("Falha ao processar vagas da empresa '{}' no Lever: {}", empresa, e.getMessage(), e);
            }
        }

        log.info("LeverCrawler coletou {} vaga(s) de estágio/júnior em Java", vagas.size());
        return vagas;
    }

    private void processarPosting(LeverPostingDto posting, String empresa, Set<String> idsVistos, List<Vaga> vagas) {
        if (posting.id() == null || posting.text() == null || posting.hostedUrl() == null || !idsVistos.add(posting.id())) {
            return;
        }
        Optional<NivelVaga> nivel = classifier.classificarNivel(posting);
        if (nivel.isEmpty() || !classifier.isRelevanteParaTech(posting)) {
            return;
        }
        if (!classifier.isRemota(posting) || !classifier.isJava(posting)) {
            return;
        }
        vagas.add(mapper.paraVaga(posting, empresa, nivel.get()));
    }

    private boolean podeColetar() {
        boolean permitido = robotsTxtChecker.isPermitido(properties.getBaseUrl(), CAMINHO_API);
        if (!permitido) {
            log.warn("Coleta do Lever abortada: robots.txt não permite acesso a {}", CAMINHO_API);
        }
        return permitido;
    }
}
