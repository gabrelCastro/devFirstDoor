package com.devfirstdoor.crawler.linkedin;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.devfirstdoor.service.ConfiguracaoColeta;
import com.devfirstdoor.service.ConfiguracaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class LinkedinHtmlClient {

    private static final Logger log = LoggerFactory.getLogger(LinkedinHtmlClient.class);
    private static final String CAMINHO_BUSCA = "/jobs-guest/jobs/api/seeMoreJobPostings/search";
    private static final String CAMINHO_DETALHE = "/jobs-guest/jobs/api/jobPosting/";

    private final LinkedinCrawlerProperties properties;
    private final ConfiguracaoService configuracaoService;
    private final LinkedinJobParser parser = new LinkedinJobParser();

    @Autowired
    public LinkedinHtmlClient(LinkedinCrawlerProperties properties, ConfiguracaoService configuracaoService) {
        this.properties = properties;
        this.configuracaoService = configuracaoService;
    }

    public LinkedinHtmlClient(LinkedinCrawlerProperties properties) {
        this(properties, null);
    }

    /**
     * Pagina a busca pública de um termo até uma página vazia ou o limite configurado.
     * A paginação é por offset ({@code start}), avançando pela quantidade de cards recebidos.
     *
     * @throws LinkedinBloqueadoException se o LinkedIn responder 429/999 (limite de acesso),
     *                                    para que o crawler interrompa os termos restantes.
     */
    public List<LinkedinJobDto> buscarTodasAsPaginas(String termo) {
        List<LinkedinJobDto> resultado = new ArrayList<>();
        int start = 0;

        for (int pagina = 1; pagina <= properties.getMaxPaginasPorTermo(); pagina++) {
            aguardarEntreRequisicoes();
            Document documento = buscarPagina(termo, start);
            if (documento == null) {
                break;
            }
            List<LinkedinJobDto> jobs = parser.parse(documento, properties.getBaseUrl());
            if (jobs.isEmpty()) {
                break;
            }
            resultado.addAll(jobs);
            start += jobs.size();
        }
        return resultado;
    }

    /**
     * Texto da descrição da vaga, ou null se a página não pôde ser lida.
     *
     * @throws LinkedinBloqueadoException se o LinkedIn responder 429/999
     */
    public String buscarDescricao(String id) {
        aguardarEntreRequisicoes();
        String url = properties.getBaseUrl().replaceAll("/+$", "") + CAMINHO_DETALHE + id;
        try {
            return parser.parseDescricao(conectar(url));
        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 429 || e.getStatusCode() == 999) {
                throw new LinkedinBloqueadoException(e.getStatusCode());
            }
            log.warn("Não foi possível ler a vaga {} do LinkedIn: status {}", id, e.getStatusCode());
            return null;
        } catch (IOException e) {
            log.warn("Não foi possível ler a vaga {} do LinkedIn: {}", id, e.getMessage());
            return null;
        }
    }

    private Document buscarPagina(String termo, int start) {
        String url = montarUrl(termo, start);
        try {
            return conectar(url);
        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 429 || e.getStatusCode() == 999) {
                throw new LinkedinBloqueadoException(e.getStatusCode());
            }
            // a busca pública responde 400 quando o offset passa do total de resultados
            log.debug("Busca do LinkedIn encerrada (termo '{}', start {}): status {}", termo, start, e.getStatusCode());
            return null;
        } catch (IOException e) {
            log.error("Falha ao buscar vagas no LinkedIn (termo '{}', start {}): {}", termo, start, e.getMessage());
            return null;
        }
    }

    private Document conectar(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(properties.getUserAgent())
                .header("Accept-Language", "pt-BR,pt;q=0.9")
                .timeout(10_000)
                .get();
    }

    String montarUrl(String termo, int start) {
        StringBuilder url = new StringBuilder(properties.getBaseUrl().replaceAll("/+$", ""))
                .append(CAMINHO_BUSCA)
                .append("?keywords=").append(URLEncoder.encode(termo, StandardCharsets.UTF_8))
                .append("&geoId=").append(properties.getGeoId());
        if (properties.isApenasRemoto()) {
            url.append("&f_WT=2");
        }
        return url.append("&start=").append(start).toString();
    }

    private void aguardarEntreRequisicoes() {
        ConfiguracaoColeta configuracao = configuracaoService != null ? configuracaoService.obter() : null;
        long pausa = configuracao != null ? configuracao.pausaLinkedinMs() : properties.getRequestDelayMs();
        long variacao = configuracao != null ? configuracao.variacaoPausaLinkedinMs() : properties.getRequestJitterMs();
        long jitter = variacao > 0
                ? ThreadLocalRandom.current().nextLong(variacao)
                : 0;
        try {
            Thread.sleep(pausa + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class LinkedinBloqueadoException extends RuntimeException {
        public LinkedinBloqueadoException(int status) {
            super("LinkedIn limitou o acesso (status " + status + ")");
        }
    }
}
