package com.devfirstdoor.crawler.greenhouse;

import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.crawler.greenhouse.dto.GreenhouseJobDto;
import com.devfirstdoor.domain.MotivoDescarte;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.robots.RobotsTxtChecker;
import com.devfirstdoor.service.ConfiguracaoColeta;
import com.devfirstdoor.service.ConfiguracaoService;
import com.devfirstdoor.service.RegistroDescarte;
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
@EnableConfigurationProperties(GreenhouseCrawlerProperties.class)
public class GreenhouseCrawler implements VagaCrawler {

    private static final Logger log = LoggerFactory.getLogger(GreenhouseCrawler.class);
    private static final String CAMINHO_API = "/v1/boards/";

    private final GreenhouseApiClient apiClient;
    private final GreenhouseCrawlerProperties properties;
    private final GreenhouseVagaClassifier classifier;
    private final GreenhouseJobMapper mapper = new GreenhouseJobMapper();
    private final RobotsTxtChecker robotsTxtChecker;
    private final ConfiguracaoService configuracaoService;
    private final RegistroDescarte registroDescarte;

    @Autowired
    public GreenhouseCrawler(GreenhouseApiClient apiClient, GreenhouseCrawlerProperties properties,
                             RobotsTxtChecker robotsTxtChecker, ConfiguracaoService configuracaoService,
                             RegistroDescarte registroDescarte) {
        this.apiClient = apiClient;
        this.properties = properties;
        this.classifier = new GreenhouseVagaClassifier(properties);
        this.robotsTxtChecker = robotsTxtChecker;
        this.configuracaoService = configuracaoService;
        this.registroDescarte = registroDescarte;
    }

    public GreenhouseCrawler(GreenhouseApiClient apiClient, GreenhouseCrawlerProperties properties,
                             RobotsTxtChecker robotsTxtChecker, ConfiguracaoService configuracaoService) {
        this(apiClient, properties, robotsTxtChecker, configuracaoService, RegistroDescarte.NENHUM);
    }

    public GreenhouseCrawler(GreenhouseApiClient apiClient, GreenhouseCrawlerProperties properties,
                             RobotsTxtChecker robotsTxtChecker) {
        this(apiClient, properties, robotsTxtChecker, null, RegistroDescarte.NENHUM);
    }

    @Override
    public String getFonte() {
        return GreenhouseJobMapper.FONTE;
    }

    @Override
    public boolean isLigada() {
        if (configuracaoService == null) {
            return properties.isEnabled() && !properties.getEmpresas().isEmpty();
        }
        ConfiguracaoColeta configuracao = configuracaoService.obter();
        return configuracao.fonteLigada(getFonte()) && !configuracao.empresasGreenhouse().isEmpty();
    }

    @Override
    public List<Vaga> coletar() {
        ConfiguracaoColeta configuracao = configuracaoService != null ? configuracaoService.obter() : null;
        List<String> empresas = configuracao != null ? configuracao.empresasGreenhouse() : properties.getEmpresas();
        boolean ligada = configuracao != null ? configuracao.fonteLigada(getFonte()) : properties.isEnabled();
        if (!ligada || empresas.isEmpty() || !podeColetar()) {
            return List.of();
        }

        List<Vaga> vagas = new ArrayList<>();
        Set<Long> idsVistos = new HashSet<>();

        for (String empresa : empresas) {
            try {
                for (GreenhouseJobDto job : apiClient.buscarVagas(empresa)) {
                    processarJob(job, empresa, idsVistos, vagas);
                }
            } catch (Exception e) {
                log.error("Falha ao processar vagas da empresa '{}' no Greenhouse: {}", empresa, e.getMessage(), e);
            }
        }

        log.info("GreenhouseCrawler coletou {} vaga(s) de estágio/júnior em Java", vagas.size());
        return vagas;
    }

    private void processarJob(GreenhouseJobDto job, String empresa, Set<Long> idsVistos, List<Vaga> vagas) {
        if (job.id() == null || job.title() == null || job.absoluteUrl() == null || !idsVistos.add(job.id())) {
            return;
        }
        Optional<NivelVaga> nivel = classifier.classificarNivel(job);
        if (nivel.isEmpty()) {
            registrar(job, empresa, MotivoDescarte.NIVEL);
            return;
        }
        if (!classifier.isRelevanteParaTech(job)) {
            registrar(job, empresa, MotivoDescarte.FORA_DE_TECNOLOGIA);
            return;
        }
        if (!classifier.isRemota(job)) {
            registrar(job, empresa, MotivoDescarte.NAO_REMOTA);
            return;
        }
        if (!classifier.isJava(job)) {
            registrar(job, empresa, MotivoDescarte.NAO_JAVA);
            return;
        }
        vagas.add(mapper.paraVaga(job, empresa, nivel.get()));
    }

    private void registrar(GreenhouseJobDto job, String empresa, MotivoDescarte motivo) {
        String nomeEmpresa = job.companyName() != null && !job.companyName().isBlank()
                ? job.companyName() : empresa;
        registroDescarte.registrar(getFonte(), job.title(), nomeEmpresa, job.nomeDoLocal(),
                job.absoluteUrl(), motivo);
    }

    private boolean podeColetar() {
        boolean permitido = robotsTxtChecker.isPermitido(properties.getBaseUrl(), CAMINHO_API);
        if (!permitido) {
            log.warn("Coleta do Greenhouse abortada: robots.txt não permite acesso a {}", CAMINHO_API);
        }
        return permitido;
    }
}
