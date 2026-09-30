package com.devfirstdoor.crawler.lever;

import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.crawler.lever.dto.LeverPostingDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.robots.RobotsTxtChecker;
import com.devfirstdoor.service.ConfiguracaoColeta;
import com.devfirstdoor.service.ConfiguracaoService;
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
@EnableConfigurationProperties(LeverCrawlerProperties.class)
public class LeverCrawler implements VagaCrawler {

    private static final Logger log = LoggerFactory.getLogger(LeverCrawler.class);
    private static final String CAMINHO_API = "/v0/postings/";

    private final LeverApiClient apiClient;
    private final LeverCrawlerProperties properties;
    private final LeverVagaClassifier classifier;
    private final LeverJobMapper mapper = new LeverJobMapper();
    private final RobotsTxtChecker robotsTxtChecker;
    private final ConfiguracaoService configuracaoService;

    @Autowired
    public LeverCrawler(LeverApiClient apiClient, LeverCrawlerProperties properties, RobotsTxtChecker robotsTxtChecker,
                        ConfiguracaoService configuracaoService) {
        this.apiClient = apiClient;
        this.properties = properties;
        this.classifier = new LeverVagaClassifier(properties);
        this.robotsTxtChecker = robotsTxtChecker;
        this.configuracaoService = configuracaoService;
    }

    public LeverCrawler(LeverApiClient apiClient, LeverCrawlerProperties properties,
                        RobotsTxtChecker robotsTxtChecker) {
        this(apiClient, properties, robotsTxtChecker, null);
    }

    @Override
    public String getFonte() {
        return LeverJobMapper.FONTE;
    }

    @Override
    public boolean isLigada() {
        if (configuracaoService == null) {
            return properties.isEnabled() && !properties.getEmpresas().isEmpty();
        }
        ConfiguracaoColeta configuracao = configuracaoService.obter();
        return configuracao.fonteLigada(getFonte()) && !configuracao.empresasLever().isEmpty();
    }

    @Override
    public List<Vaga> coletar() {
        ConfiguracaoColeta configuracao = configuracaoService != null ? configuracaoService.obter() : null;
        List<String> empresas = configuracao != null ? configuracao.empresasLever() : properties.getEmpresas();
        boolean ligada = configuracao != null ? configuracao.fonteLigada(getFonte()) : properties.isEnabled();
        if (!ligada || empresas.isEmpty() || !podeColetar()) {
            return List.of();
        }

        List<Vaga> vagas = new ArrayList<>();
        Set<String> idsVistos = new HashSet<>();

        for (String empresa : empresas) {
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
