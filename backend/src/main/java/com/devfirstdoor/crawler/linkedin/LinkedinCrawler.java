package com.devfirstdoor.crawler.linkedin;

import com.devfirstdoor.crawler.ProgressoColeta;
import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.crawler.linkedin.LinkedinHtmlClient.LinkedinBloqueadoException;
import com.devfirstdoor.domain.MotivoDescarte;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.service.DeduplicacaoService;
import com.devfirstdoor.service.ConfiguracaoColeta;
import com.devfirstdoor.service.ConfiguracaoService;
import com.devfirstdoor.service.RegistroDescarte;
import com.devfirstdoor.util.LinguagemJava;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coleta a busca pública de vagas do LinkedIn (a mesma que um visitante deslogado vê),
 * sem conta e sem login.
 *
 * Diferente dos outros crawlers, este NÃO consulta o {@code RobotsTxtChecker}: o
 * robots.txt do LinkedIn proíbe esses caminhos. Por isso a coleta só acontece com
 * {@code LINKEDIN_ENABLED=true} e a fonte ligada no painel, uma escolha explícita para
 * uso pessoal e em baixo volume. Se o LinkedIn limitar o acesso (429/999), a coleta desta
 * execução é interrompida em vez de insistir.
 *
 * A busca não informa a modalidade (remoto/híbrido/presencial) nem os requisitos, então
 * cada vaga ainda não salva tem a página de detalhe lida para classificar a modalidade e
 * confirmar que pede Java. Vagas já existentes no banco, ou já lidas e descartadas por
 * não pedirem Java, são puladas antes disso, para não repetir requisições a cada coleta.
 */
@Component
@EnableConfigurationProperties(LinkedinCrawlerProperties.class)
public class LinkedinCrawler implements VagaCrawler {

    private static final Logger log = LoggerFactory.getLogger(LinkedinCrawler.class);

    private final LinkedinHtmlClient client;
    private final LinkedinCrawlerProperties properties;
    private final LinkedinVagaClassifier classifier;
    private final LinkedinJobMapper mapper = new LinkedinJobMapper();
    private final DeduplicacaoService deduplicacaoService;
    private final ConfiguracaoService configuracaoService;
    private final RegistroDescarte registroDescarte;
    private final Set<String> hashesSemJava = ConcurrentHashMap.newKeySet();
    private volatile int jaSalvasNaUltimaColeta;

    @Autowired
    public LinkedinCrawler(LinkedinHtmlClient client, LinkedinCrawlerProperties properties,
                           DeduplicacaoService deduplicacaoService, ConfiguracaoService configuracaoService,
                           RegistroDescarte registroDescarte) {
        this.client = client;
        this.properties = properties;
        this.classifier = new LinkedinVagaClassifier(properties);
        this.deduplicacaoService = deduplicacaoService;
        this.configuracaoService = configuracaoService;
        this.registroDescarte = registroDescarte;
    }

    public LinkedinCrawler(LinkedinHtmlClient client, LinkedinCrawlerProperties properties,
                           DeduplicacaoService deduplicacaoService, ConfiguracaoService configuracaoService) {
        this(client, properties, deduplicacaoService, configuracaoService, RegistroDescarte.NENHUM);
    }

    public LinkedinCrawler(LinkedinHtmlClient client, LinkedinCrawlerProperties properties,
                           DeduplicacaoService deduplicacaoService) {
        this(client, properties, deduplicacaoService, null, RegistroDescarte.NENHUM);
    }

    public LinkedinCrawler(LinkedinHtmlClient client, LinkedinCrawlerProperties properties,
                           DeduplicacaoService deduplicacaoService, RegistroDescarte registroDescarte) {
        this(client, properties, deduplicacaoService, null, registroDescarte);
    }

    @Override
    public String getFonte() {
        return LinkedinJobMapper.FONTE;
    }

    @Override
    public boolean isLigada() {
        return configuracaoService == null || configuracaoService.obter().fonteLigada(getFonte());
    }

    @Override
    public List<Vaga> coletar() {
        return coletar(ProgressoColeta.NENHUM);
    }

    @Override
    public List<Vaga> coletar(ProgressoColeta progresso) {
        ConfiguracaoColeta configuracao = configuracaoService != null ? configuracaoService.obter() : null;
        if (configuracao != null && !configuracao.fonteLigada(getFonte())) {
            return List.of();
        }
        List<String> termos = configuracao != null ? configuracao.termosBuscaLinkedin() : properties.getTermosBusca();
        List<Candidata> candidatas = buscarCandidatas(progresso, termos);
        List<Vaga> vagas = new ArrayList<>();

        for (int i = 0; i < candidatas.size(); i++) {
            Candidata candidata = candidatas.get(i);
            progresso.informar("lendo descrições %d/%d".formatted(i + 1, candidatas.size()));
            String descricao;
            try {
                descricao = candidata.job().id() != null ? client.buscarDescricao(candidata.job().id()) : null;
            } catch (LinkedinBloqueadoException e) {
                // as vagas restantes não são devolvidas sem modalidade: ficam para a próxima coleta
                log.warn("{}; a leitura das vagas restantes fica para a próxima execução", e.getMessage());
                break;
            }
            if (descricao == null && !LinguagemJava.mencionadaEm(candidata.job().titulo())) {
                registrar(candidata.job(), MotivoDescarte.ERRO_LEITURA);
                continue;
            }
            if (!LinguagemJava.mencionadaEm(candidata.job().titulo(), descricao)) {
                if (descricao != null) {
                    hashesSemJava.add(candidata.hash());
                }
                registrar(candidata.job(), MotivoDescarte.NAO_JAVA);
                continue;
            }
            LinkedinModalidade modalidade = classifier.classificarModalidade(candidata.job().titulo(), descricao);
            vagas.add(mapper.paraVaga(candidata.job(), candidata.nivel(), modalidade));
        }

        log.info("LinkedinCrawler coletou {} vaga(s) nova(s) de estágio/júnior em Java", vagas.size());
        return vagas;
    }

    /** As devolvidas são só as novas; as já salvas que continuam na busca também contam. */
    @Override
    public int contarEncontradas(List<Vaga> devolvidas) {
        return devolvidas.size() + jaSalvasNaUltimaColeta;
    }

    /**
     * Vagas de estágio/júnior em tecnologia que ainda não existem no banco. As que já
     * existem não são devolvidas (para não reler a descrição), então têm a visita
     * registrada aqui, senão expirariam mesmo continuando na busca.
     */
    private List<Candidata> buscarCandidatas(ProgressoColeta progresso, List<String> termos) {
        List<Candidata> candidatas = new ArrayList<>();
        Set<String> hashesVistos = new HashSet<>();
        Set<String> hashesJaSalvos = new HashSet<>();
        LocalDateTime inicio = LocalDateTime.now();
        for (int i = 0; i < termos.size(); i++) {
            String termo = termos.get(i);
            progresso.informar("termo %d/%d".formatted(i + 1, termos.size()));
            try {
                for (LinkedinJobDto job : client.buscarTodasAsPaginas(termo)) {
                    avaliar(job, hashesVistos, hashesJaSalvos).ifPresent(candidatas::add);
                }
            } catch (LinkedinBloqueadoException e) {
                log.warn("{}; termos restantes ficam para a próxima execução", e.getMessage());
                break;
            } catch (Exception e) {
                log.error("Falha ao coletar o termo '{}' no LinkedIn: {}", termo, e.getMessage(), e);
            }
        }
        deduplicacaoService.registrarVisita(hashesJaSalvos, inicio);
        jaSalvasNaUltimaColeta = hashesJaSalvos.size();
        return candidatas;
    }

    private Optional<Candidata> avaliar(LinkedinJobDto job, Set<String> hashesVistos, Set<String> hashesJaSalvos) {
        String hash = deduplicacaoService.calcularHash(job.titulo(), job.empresa(), LinkedinJobMapper.FONTE);
        if (!hashesVistos.add(hash)) {
            return Optional.empty();
        }
        if (hashesSemJava.contains(hash)) {
            registrar(job, MotivoDescarte.NAO_JAVA);
            return Optional.empty();
        }
        if (deduplicacaoService.isDuplicada(hash)) {
            hashesJaSalvos.add(hash);
            return Optional.empty();
        }
        Optional<NivelVaga> nivel = classifier.classificarNivel(job);
        if (nivel.isEmpty()) {
            registrar(job, MotivoDescarte.NIVEL);
            return Optional.empty();
        }
        if (!classifier.isRelevanteParaTech(job)) {
            registrar(job, MotivoDescarte.FORA_DE_TECNOLOGIA);
            return Optional.empty();
        }
        return Optional.of(new Candidata(job, nivel.get(), hash));
    }

    private void registrar(LinkedinJobDto job, MotivoDescarte motivo) {
        registroDescarte.registrar(getFonte(), job.titulo(), job.empresa(), job.local(), job.link(), motivo);
    }

    private record Candidata(LinkedinJobDto job, NivelVaga nivel, String hash) {
    }
}
