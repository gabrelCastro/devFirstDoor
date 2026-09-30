package com.devfirstdoor.crawler.linkedin;

import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.crawler.linkedin.LinkedinHtmlClient.LinkedinBloqueadoException;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.service.DeduplicacaoService;
import com.devfirstdoor.util.LinguagemJava;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * robots.txt do LinkedIn proíbe esses caminhos. Por isso só é registrado com
 * {@code app.crawler.linkedin.enabled=true}, uma escolha explícita para uso pessoal
 * e em baixo volume. Se o LinkedIn limitar o acesso (429/999), a coleta desta
 * execução é interrompida em vez de insistir.
 *
 * A busca não informa a modalidade (remoto/híbrido/presencial) nem os requisitos, então
 * cada vaga ainda não salva tem a página de detalhe lida para classificar a modalidade e
 * confirmar que pede Java. Vagas já existentes no banco, ou já lidas e descartadas por
 * não pedirem Java, são puladas antes disso, para não repetir requisições a cada coleta.
 */
@Component
@ConditionalOnProperty(prefix = "app.crawler.linkedin", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(LinkedinCrawlerProperties.class)
public class LinkedinCrawler implements VagaCrawler {

    private static final Logger log = LoggerFactory.getLogger(LinkedinCrawler.class);

    private final LinkedinHtmlClient client;
    private final LinkedinCrawlerProperties properties;
    private final LinkedinVagaClassifier classifier;
    private final LinkedinJobMapper mapper = new LinkedinJobMapper();
    private final DeduplicacaoService deduplicacaoService;
    private final Set<String> hashesSemJava = ConcurrentHashMap.newKeySet();
    private volatile int jaSalvasNaUltimaColeta;

    public LinkedinCrawler(LinkedinHtmlClient client, LinkedinCrawlerProperties properties,
                           DeduplicacaoService deduplicacaoService) {
        this.client = client;
        this.properties = properties;
        this.classifier = new LinkedinVagaClassifier(properties);
        this.deduplicacaoService = deduplicacaoService;
    }

    @Override
    public String getFonte() {
        return LinkedinJobMapper.FONTE;
    }

    @Override
    public List<Vaga> coletar() {
        List<Candidata> candidatas = buscarCandidatas();
        List<Vaga> vagas = new ArrayList<>();

        for (Candidata candidata : candidatas) {
            String descricao;
            try {
                descricao = candidata.job().id() != null ? client.buscarDescricao(candidata.job().id()) : null;
            } catch (LinkedinBloqueadoException e) {
                // as vagas restantes não são devolvidas sem modalidade: ficam para a próxima coleta
                log.warn("{}; a leitura das vagas restantes fica para a próxima execução", e.getMessage());
                break;
            }
            if (!LinguagemJava.mencionadaEm(candidata.job().titulo(), descricao)) {
                if (descricao != null) {
                    hashesSemJava.add(candidata.hash());
                }
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
    private List<Candidata> buscarCandidatas() {
        List<Candidata> candidatas = new ArrayList<>();
        Set<String> hashesVistos = new HashSet<>();
        Set<String> hashesJaSalvos = new HashSet<>();
        LocalDateTime inicio = LocalDateTime.now();

        for (String termo : properties.getTermosBusca()) {
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
        if (!hashesVistos.add(hash) || hashesSemJava.contains(hash)) {
            return Optional.empty();
        }
        if (deduplicacaoService.isDuplicada(hash)) {
            hashesJaSalvos.add(hash);
            return Optional.empty();
        }
        Optional<NivelVaga> nivel = classifier.classificarNivel(job);
        if (nivel.isEmpty() || !classifier.isRelevanteParaTech(job)) {
            return Optional.empty();
        }
        return Optional.of(new Candidata(job, nivel.get(), hash));
    }

    private record Candidata(LinkedinJobDto job, NivelVaga nivel, String hash) {
    }
}
