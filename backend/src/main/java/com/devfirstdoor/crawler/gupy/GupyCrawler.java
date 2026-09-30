package com.devfirstdoor.crawler.gupy;

import com.devfirstdoor.crawler.ProgressoColeta;
import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.crawler.gupy.dto.GupyJobDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.robots.RobotsTxtChecker;
import com.devfirstdoor.service.ConfiguracaoColeta;
import com.devfirstdoor.service.ConfiguracaoService;
import com.devfirstdoor.util.LinguagemJava;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableConfigurationProperties(GupyCrawlerProperties.class)
public class GupyCrawler implements VagaCrawler {

    private static final Logger log = LoggerFactory.getLogger(GupyCrawler.class);

    private final GupyApiClient apiClient;
    private final GupyCrawlerProperties properties;
    private final GupyVagaClassifier classifier;
    private final GupyJobMapper mapper = new GupyJobMapper();
    private final RobotsTxtChecker robotsTxtChecker;
    private final ConfiguracaoService configuracaoService;

    /**
     * Resultado da leitura da página de cada vaga (menciona Java ou não). Fica em memória
     * para que cada coleta não releia as mesmas páginas (a descrição raramente muda).
     */
    private final Map<Long, Boolean> javaPorId = new ConcurrentHashMap<>();

    @Autowired
    public GupyCrawler(GupyApiClient apiClient, GupyCrawlerProperties properties, RobotsTxtChecker robotsTxtChecker,
                       ConfiguracaoService configuracaoService) {
        this.apiClient = apiClient;
        this.properties = properties;
        this.classifier = new GupyVagaClassifier(properties);
        this.robotsTxtChecker = robotsTxtChecker;
        this.configuracaoService = configuracaoService;
    }

    public GupyCrawler(GupyApiClient apiClient, GupyCrawlerProperties properties, RobotsTxtChecker robotsTxtChecker) {
        this(apiClient, properties, robotsTxtChecker, null);
    }

    @Override
    public String getFonte() {
        return GupyJobMapper.FONTE;
    }

    @Override
    public boolean isLigada() {
        return configuracaoService == null ? properties.isEnabled()
                : configuracaoService.obter().fonteLigada(getFonte());
    }

    @Override
    public List<Vaga> coletar() {
        return coletar(ProgressoColeta.NENHUM);
    }

    @Override
    public List<Vaga> coletar(ProgressoColeta progresso) {
        ConfiguracaoColeta configuracao = configuracaoService != null ? configuracaoService.obter() : null;
        if (!(configuracao != null ? configuracao.fonteLigada(getFonte()) : properties.isEnabled()) || !podeColetar()) {
            return List.of();
        }

        List<Vaga> vagas = new ArrayList<>();
        Set<Long> idsVistos = new HashSet<>();
        List<String> termos = configuracao != null ? configuracao.termosBuscaGupy() : properties.getTermosBusca();

        for (int i = 0; i < termos.size(); i++) {
            String termo = termos.get(i);
            progresso.informar("termo %d/%d".formatted(i + 1, termos.size()));
            try {
                List<GupyJobDto> jobs = apiClient.buscarTodasAsPaginas(termo);
                for (GupyJobDto job : jobs) {
                    processarJob(job, idsVistos, vagas);
                }
            } catch (Exception e) {
                log.error("Falha ao processar termo de busca '{}' na Gupy: {}", termo, e.getMessage(), e);
            }
        }

        log.info("GupyCrawler coletou {} vaga(s) de estágio/júnior em Java", vagas.size());
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
        if (!isJava(job)) {
            return;
        }
        vagas.add(mapper.paraVaga(job, nivel.get()));
    }

    /** O título basta quando cita Java; senão, a página da vaga é lida atrás dos requisitos. */
    private boolean isJava(GupyJobDto job) {
        if (LinguagemJava.mencionadaEm(job.name())) {
            return true;
        }
        Boolean jaLido = javaPorId.get(job.id());
        if (jaLido != null) {
            return jaLido;
        }
        if (job.jobUrl() == null) {
            return false;
        }
        String texto = apiClient.buscarTextoDaVaga(job.jobUrl());
        if (texto == null) {
            return false; // falha de leitura: não entra agora, mas é tentada de novo na próxima coleta
        }
        boolean java = LinguagemJava.mencionadaEm(texto);
        javaPorId.put(job.id(), java);
        return java;
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
