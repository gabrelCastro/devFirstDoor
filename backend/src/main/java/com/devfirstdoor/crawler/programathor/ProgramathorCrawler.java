package com.devfirstdoor.crawler.programathor;

import com.devfirstdoor.crawler.VagaCrawler;
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
import java.util.Set;

/**
 * A ProgramaThor é um quadro de vagas só para programadores, com filtros nativos
 * de tipo de contrato, nível de experiência e trabalho remoto — diferente da Gupy,
 * aqui não é preciso adivinhar nível nem relevância para tech por palavra-chave.
 */
@Component
@EnableConfigurationProperties(ProgramathorCrawlerProperties.class)
public class ProgramathorCrawler implements VagaCrawler {

    private static final Logger log = LoggerFactory.getLogger(ProgramathorCrawler.class);

    private static final String FILTRO_ESTAGIO_REMOTO = "contract_type=Est%C3%A1gio&remoto=true";
    private static final String FILTRO_JUNIOR_REMOTO = "expertise=J%C3%BAnior&remoto=true";

    private final ProgramathorHtmlClient client;
    private final ProgramathorCrawlerProperties properties;
    private final ProgramathorJobMapper mapper = new ProgramathorJobMapper();
    private final RobotsTxtChecker robotsTxtChecker;
    private final ConfiguracaoService configuracaoService;
    private final RegistroDescarte registroDescarte;

    @Autowired
    public ProgramathorCrawler(ProgramathorHtmlClient client, ProgramathorCrawlerProperties properties,
                                RobotsTxtChecker robotsTxtChecker, ConfiguracaoService configuracaoService,
                                RegistroDescarte registroDescarte) {
        this.client = client;
        this.properties = properties;
        this.robotsTxtChecker = robotsTxtChecker;
        this.configuracaoService = configuracaoService;
        this.registroDescarte = registroDescarte;
    }

    public ProgramathorCrawler(ProgramathorHtmlClient client, ProgramathorCrawlerProperties properties,
                               RobotsTxtChecker robotsTxtChecker, ConfiguracaoService configuracaoService) {
        this(client, properties, robotsTxtChecker, configuracaoService, RegistroDescarte.NENHUM);
    }

    public ProgramathorCrawler(ProgramathorHtmlClient client, ProgramathorCrawlerProperties properties,
                               RobotsTxtChecker robotsTxtChecker) {
        this(client, properties, robotsTxtChecker, null, RegistroDescarte.NENHUM);
    }

    public ProgramathorCrawler(ProgramathorHtmlClient client, ProgramathorCrawlerProperties properties,
                               RobotsTxtChecker robotsTxtChecker, RegistroDescarte registroDescarte) {
        this(client, properties, robotsTxtChecker, null, registroDescarte);
    }

    @Override
    public String getFonte() {
        return ProgramathorJobMapper.FONTE;
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
        Set<String> linksVistos = new HashSet<>();

        coletarFiltro(FILTRO_ESTAGIO_REMOTO, NivelVaga.ESTAGIO, vagas, linksVistos);
        coletarFiltro(FILTRO_JUNIOR_REMOTO, NivelVaga.JUNIOR, vagas, linksVistos);

        log.info("ProgramathorCrawler coletou {} vaga(s) remota(s) de estágio/júnior em Java", vagas.size());
        return vagas;
    }

    private void coletarFiltro(String queryString, NivelVaga nivel, List<Vaga> vagas, Set<String> linksVistos) {
        try {
            List<ProgramathorJobDto> jobs = client.buscarTodasAsPaginas(queryString);
            for (ProgramathorJobDto job : jobs) {
                if (!LinguagemJava.mencionadaEm(job.tags()) && !LinguagemJava.mencionadaEm(job.titulo())) {
                    registroDescarte.registrar(getFonte(), job.titulo(), job.empresa(), job.local(), job.link(),
                            MotivoDescarte.NAO_JAVA);
                    continue;
                }
                if (linksVistos.add(job.link())) {
                    vagas.add(mapper.paraVaga(job, nivel));
                }
            }
        } catch (Exception e) {
            log.error("Falha ao coletar o filtro '{}' na ProgramaThor: {}", queryString, e.getMessage(), e);
        }
    }

    private boolean podeColetar() {
        boolean permitido = robotsTxtChecker.isPermitido(properties.getBaseUrl(), "/jobs");
        if (!permitido) {
            log.warn("Coleta da ProgramaThor abortada: robots.txt não permite acesso a /jobs");
        }
        return permitido;
    }
}
