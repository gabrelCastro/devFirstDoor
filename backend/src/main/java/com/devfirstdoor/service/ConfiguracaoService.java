package com.devfirstdoor.service;

import com.devfirstdoor.controller.dto.ConfiguracaoAdminRequest;
import com.devfirstdoor.controller.dto.ConfiguracaoAdminResponse;
import com.devfirstdoor.crawler.greenhouse.GreenhouseCrawlerProperties;
import com.devfirstdoor.crawler.gupy.GupyCrawlerProperties;
import com.devfirstdoor.crawler.lever.LeverCrawlerProperties;
import com.devfirstdoor.crawler.linkedin.LinkedinCrawlerProperties;
import com.devfirstdoor.crawler.programathor.ProgramathorCrawlerProperties;
import com.devfirstdoor.crawler.remoteok.RemoteOkCrawlerProperties;
import com.devfirstdoor.domain.Configuracao;
import com.devfirstdoor.repository.ConfiguracaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Centraliza os padrões do yml e as substituições persistidas feitas no painel. */
@Service
public class ConfiguracaoService {

    public static final List<String> FONTES = List.of(
            "GUPY", "PROGRAMATHOR", "REMOTEOK", "LINKEDIN", "GREENHOUSE", "LEVER");

    private static final String FONTES_LIGADAS = "fontes.ligadas";
    private static final String TERMOS_GUPY = "gupy.termos-busca";
    private static final String TERMOS_LINKEDIN = "linkedin.termos-busca";
    private static final String EMPRESAS_GREENHOUSE = "greenhouse.empresas";
    private static final String EMPRESAS_LEVER = "lever.empresas";
    private static final String INTERVALO_MINUTOS = "coleta.intervalo-minutos";
    private static final String DIAS_EXPIRAR = "coleta.dias-para-expirar";
    private static final String PAUSA_LINKEDIN = "linkedin.pausa-ms";
    private static final String VARIACAO_PAUSA_LINKEDIN = "linkedin.variacao-pausa-ms";
    private static final String AGENDAMENTO_PAUSADO = "agendamento.pausado";

    private final ConfiguracaoRepository repository;
    private final ObjectMapper objectMapper;
    private final GupyCrawlerProperties gupy;
    private final LinkedinCrawlerProperties linkedin;
    private final GreenhouseCrawlerProperties greenhouse;
    private final LeverCrawlerProperties lever;
    private final ProgramathorCrawlerProperties programathor;
    private final RemoteOkCrawlerProperties remoteok;
    private final Duration intervaloPadrao;
    private final int diasParaExpirarPadrao;

    public ConfiguracaoService(ConfiguracaoRepository repository, ObjectMapper objectMapper,
                               GupyCrawlerProperties gupy, LinkedinCrawlerProperties linkedin,
                               GreenhouseCrawlerProperties greenhouse, LeverCrawlerProperties lever,
                               ProgramathorCrawlerProperties programathor, RemoteOkCrawlerProperties remoteok,
                               org.springframework.core.env.Environment environment) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.gupy = gupy;
        this.linkedin = linkedin;
        this.greenhouse = greenhouse;
        this.lever = lever;
        this.programathor = programathor;
        this.remoteok = remoteok;
        this.intervaloPadrao = environment.getProperty("app.crawler.intervalo", Duration.class, Duration.ofHours(6));
        this.diasParaExpirarPadrao = environment.getProperty("app.crawler.dias-para-expirar", Integer.class, 7);
    }

    /** Lê o banco novamente; por isso uma alteração vale já para a coleta seguinte. */
    @Transactional(readOnly = true)
    public ConfiguracaoColeta obter() {
        Map<String, String> valores = new LinkedHashMap<>();
        repository.findAll().forEach(item -> valores.put(item.getChave(), item.getValorJson()));

        Map<String, Boolean> fontes = ler(valores, FONTES_LIGADAS, new TypeReference<>() {}, fontesPadrao());
        return new ConfiguracaoColeta(
                Map.copyOf(fontes),
                List.copyOf(ler(valores, TERMOS_GUPY, new TypeReference<>() {}, gupy.getTermosBusca())),
                List.copyOf(ler(valores, TERMOS_LINKEDIN, new TypeReference<>() {}, linkedin.getTermosBusca())),
                List.copyOf(ler(valores, EMPRESAS_GREENHOUSE, new TypeReference<>() {}, greenhouse.getEmpresas())),
                List.copyOf(ler(valores, EMPRESAS_LEVER, new TypeReference<>() {}, lever.getEmpresas())),
                Duration.ofMinutes(ler(valores, INTERVALO_MINUTOS, Long.class, intervaloPadrao.toMinutes())),
                ler(valores, DIAS_EXPIRAR, Integer.class, diasParaExpirarPadrao),
                ler(valores, PAUSA_LINKEDIN, Long.class, linkedin.getRequestDelayMs()),
                ler(valores, VARIACAO_PAUSA_LINKEDIN, Long.class, linkedin.getRequestJitterMs()),
                ler(valores, AGENDAMENTO_PAUSADO, Boolean.class, false),
                linkedin.isEnabled()
        );
    }

    @Transactional(readOnly = true)
    public ConfiguracaoAdminResponse consultar() {
        return resposta(obter());
    }

    @Transactional
    public ConfiguracaoAdminResponse salvar(ConfiguracaoAdminRequest request) {
        validar(request);
        Map<String, Boolean> fontes = new LinkedHashMap<>();
        FONTES.forEach(fonte -> fontes.put(fonte, request.fontesLigadas().get(fonte)));

        repository.saveAll(List.of(
                item(FONTES_LIGADAS, fontes),
                item(TERMOS_GUPY, request.termosBuscaGupy()),
                item(TERMOS_LINKEDIN, request.termosBuscaLinkedin()),
                item(EMPRESAS_GREENHOUSE, request.empresasGreenhouse()),
                item(EMPRESAS_LEVER, request.empresasLever()),
                item(INTERVALO_MINUTOS, request.intervaloColetaMinutos()),
                item(DIAS_EXPIRAR, request.diasParaExpirar()),
                item(PAUSA_LINKEDIN, request.pausaLinkedinMs()),
                item(VARIACAO_PAUSA_LINKEDIN, request.variacaoPausaLinkedinMs()),
                item(AGENDAMENTO_PAUSADO, request.agendamentoPausado())
        ));
        return resposta(obter());
    }

    @Transactional
    public void definirAgendamentoPausado(boolean pausado) {
        repository.save(item(AGENDAMENTO_PAUSADO, pausado));
    }

    private Map<String, Boolean> fontesPadrao() {
        Map<String, Boolean> fontes = new LinkedHashMap<>();
        fontes.put("GUPY", gupy.isEnabled());
        fontes.put("PROGRAMATHOR", programathor.isEnabled());
        fontes.put("REMOTEOK", remoteok.isEnabled());
        fontes.put("LINKEDIN", linkedin.isEnabled());
        fontes.put("GREENHOUSE", greenhouse.isEnabled());
        fontes.put("LEVER", lever.isEnabled());
        return fontes;
    }

    private void validar(ConfiguracaoAdminRequest request) {
        if (request == null || request.fontesLigadas() == null
                || FONTES.stream().anyMatch(fonte -> request.fontesLigadas().get(fonte) == null)) {
            throw new IllegalArgumentException("Informe o estado ligado/desligado de todas as fontes");
        }
        validarLista("termos de busca da Gupy", request.termosBuscaGupy());
        validarLista("termos de busca do LinkedIn", request.termosBuscaLinkedin());
        validarLista("empresas do Greenhouse", request.empresasGreenhouse());
        validarLista("empresas do Lever", request.empresasLever());
        if (request.intervaloColetaMinutos() == null || request.intervaloColetaMinutos() < 30) {
            throw new IllegalArgumentException("O intervalo da coleta deve ser de pelo menos 30 minutos");
        }
        if (request.diasParaExpirar() == null || request.diasParaExpirar() < 1) {
            throw new IllegalArgumentException("Os dias para expirar devem ser pelo menos 1");
        }
        if (request.pausaLinkedinMs() == null || request.pausaLinkedinMs() < 0
                || request.variacaoPausaLinkedinMs() == null || request.variacaoPausaLinkedinMs() < 0) {
            throw new IllegalArgumentException("As pausas do LinkedIn não podem ser negativas");
        }
        if (request.agendamentoPausado() == null) {
            throw new IllegalArgumentException("Informe se o agendamento está pausado");
        }
        if (Boolean.TRUE.equals(request.fontesLigadas().get("LINKEDIN")) && !linkedin.isEnabled()) {
            throw new IllegalArgumentException("O LinkedIn só pode ser ligado quando LINKEDIN_ENABLED=true");
        }
    }

    private static void validarLista(String nome, List<String> valores) {
        if (valores == null) {
            throw new IllegalArgumentException("Informe " + nome);
        }
        if (valores.stream().anyMatch(valor -> valor == null || valor.isBlank())) {
            throw new IllegalArgumentException("A lista de " + nome + " não pode conter itens vazios");
        }
    }

    private ConfiguracaoAdminResponse resposta(ConfiguracaoColeta configuracao) {
        Map<String, Boolean> fontesEfetivas = new LinkedHashMap<>();
        FONTES.forEach(fonte -> fontesEfetivas.put(fonte, configuracao.fonteLigada(fonte)));
        return new ConfiguracaoAdminResponse(
                fontesEfetivas,
                configuracao.termosBuscaGupy(),
                configuracao.termosBuscaLinkedin(),
                configuracao.empresasGreenhouse(),
                configuracao.empresasLever(),
                configuracao.intervaloColeta().toMinutes(),
                configuracao.diasParaExpirar(),
                configuracao.pausaLinkedinMs(),
                configuracao.variacaoPausaLinkedinMs(),
                configuracao.agendamentoPausado(),
                configuracao.linkedinChaveMestraAtiva()
        );
    }

    private Configuracao item(String chave, Object valor) {
        try {
            return new Configuracao(chave, objectMapper.writeValueAsString(valor));
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível serializar a configuração " + chave, e);
        }
    }

    private <T> T ler(Map<String, String> valores, String chave, Class<T> tipo, T padrao) {
        String valor = valores.get(chave);
        if (valor == null) {
            return padrao;
        }
        try {
            return objectMapper.readValue(valor, tipo);
        } catch (Exception e) {
            throw new IllegalStateException("Configuração persistida inválida: " + chave, e);
        }
    }

    private <T> T ler(Map<String, String> valores, String chave, TypeReference<T> tipo, T padrao) {
        String valor = valores.get(chave);
        if (valor == null) {
            return padrao;
        }
        try {
            return objectMapper.readValue(valor, tipo);
        } catch (Exception e) {
            throw new IllegalStateException("Configuração persistida inválida: " + chave, e);
        }
    }
}
