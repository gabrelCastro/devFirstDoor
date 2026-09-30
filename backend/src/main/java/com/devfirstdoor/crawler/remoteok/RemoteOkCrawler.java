package com.devfirstdoor.crawler.remoteok;

import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.crawler.remoteok.dto.RemoteOkJobDto;
import com.devfirstdoor.domain.MotivoDescarte;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.robots.RobotsTxtChecker;
import com.devfirstdoor.service.ConfiguracaoService;
import com.devfirstdoor.service.RegistroDescarte;
import com.devfirstdoor.util.LinguagemJava;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@EnableConfigurationProperties(RemoteOkCrawlerProperties.class)
public class RemoteOkCrawler implements VagaCrawler {

    private static final Logger log = LoggerFactory.getLogger(RemoteOkCrawler.class);

    private final RemoteOkApiClient apiClient;
    private final RemoteOkCrawlerProperties properties;
    private final RemoteOkVagaClassifier classifier;
    private final RemoteOkJobMapper mapper = new RemoteOkJobMapper();
    private final RobotsTxtChecker robotsTxtChecker;
    private final ConfiguracaoService configuracaoService;
    private final RegistroDescarte registroDescarte;

    @Autowired
    public RemoteOkCrawler(RemoteOkApiClient apiClient, RemoteOkCrawlerProperties properties,
                           RobotsTxtChecker robotsTxtChecker, ConfiguracaoService configuracaoService,
                           RegistroDescarte registroDescarte) {
        this.apiClient = apiClient;
        this.properties = properties;
        this.classifier = new RemoteOkVagaClassifier(properties);
        this.robotsTxtChecker = robotsTxtChecker;
        this.configuracaoService = configuracaoService;
        this.registroDescarte = registroDescarte;
    }

    public RemoteOkCrawler(RemoteOkApiClient apiClient, RemoteOkCrawlerProperties properties,
                           RobotsTxtChecker robotsTxtChecker, ConfiguracaoService configuracaoService) {
        this(apiClient, properties, robotsTxtChecker, configuracaoService, RegistroDescarte.NENHUM);
    }

    public RemoteOkCrawler(RemoteOkApiClient apiClient, RemoteOkCrawlerProperties properties,
                           RobotsTxtChecker robotsTxtChecker) {
        this(apiClient, properties, robotsTxtChecker, null, RegistroDescarte.NENHUM);
    }

    @Override
    public String getFonte() {
        return RemoteOkJobMapper.FONTE;
    }

    @Override
    public boolean isLigada() {
        return configuracaoService == null ? properties.isEnabled()
                : configuracaoService.obter().fonteLigada(getFonte());
    }

    @Override
    public List<Vaga> coletar() {
        if (!isLigada() || !podeColetar()) {
            return List.of();
        }

        List<Vaga> vagas = new ArrayList<>();
        Set<String> idsVistos = new HashSet<>();

        try {
            for (RemoteOkJobDto job : apiClient.buscarVagas()) {
                processarJob(job, idsVistos, vagas);
            }
        } catch (Exception e) {
            log.error("Falha ao processar vagas da RemoteOK: {}", e.getMessage(), e);
        }

        log.info("RemoteOkCrawler coletou {} vaga(s) de estágio/júnior em Java", vagas.size());
        return vagas;
    }

    private void processarJob(RemoteOkJobDto job, Set<String> idsVistos, List<Vaga> vagas) {
        if (job.id() == null || !idsVistos.add(job.id())) {
            return;
        }
        Optional<NivelVaga> nivel = classifier.classificarNivel(job);
        if (nivel.isEmpty()) {
            registrar(job, MotivoDescarte.NIVEL);
            return;
        }
        if (!classifier.isRelevanteParaTech(job)) {
            registrar(job, MotivoDescarte.FORA_DE_TECNOLOGIA);
            return;
        }
        if (!LinguagemJava.mencionadaEm(job.tags()) && !LinguagemJava.mencionadaEm(job.position())) {
            registrar(job, MotivoDescarte.NAO_JAVA);
            return;
        }
        vagas.add(mapper.paraVaga(job, nivel.get()));
    }

    private void registrar(RemoteOkJobDto job, MotivoDescarte motivo) {
        registroDescarte.registrar(getFonte(), job.position(), job.company(), job.location(), job.url(), motivo);
    }

    private boolean podeColetar() {
        boolean permitido = robotsTxtChecker.isPermitido(properties.getBaseUrl(), "/api");
        if (!permitido) {
            log.warn("Coleta da RemoteOK abortada: robots.txt não permite acesso a /api");
        }
        return permitido;
    }
}
