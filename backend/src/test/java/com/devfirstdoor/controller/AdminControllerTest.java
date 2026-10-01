package com.devfirstdoor.controller;

import com.devfirstdoor.crawler.ConsultaBoard;
import com.devfirstdoor.crawler.greenhouse.GreenhouseApiClient;
import com.devfirstdoor.crawler.greenhouse.dto.GreenhouseJobDto;
import com.devfirstdoor.crawler.greenhouse.dto.GreenhouseLocationDto;
import com.devfirstdoor.crawler.lever.LeverApiClient;
import com.devfirstdoor.domain.ExecucaoColeta;
import com.devfirstdoor.domain.ExecucaoFonte;
import com.devfirstdoor.domain.MotivoDescarte;
import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.domain.VagaDescartada;
import com.devfirstdoor.repository.ConfiguracaoRepository;
import com.devfirstdoor.repository.ExecucaoColetaRepository;
import com.devfirstdoor.repository.ExecucaoFonteRepository;
import com.devfirstdoor.repository.VagaDescartadaRepository;
import com.devfirstdoor.service.AndamentoColeta;
import com.devfirstdoor.service.ColetaService;
import com.devfirstdoor.service.ClienteTelegram;
import com.devfirstdoor.service.DescarteService;
import com.devfirstdoor.service.HistoricoColetaService;
import com.devfirstdoor.service.PausaAgendamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.admin.usuario=admin", "app.admin.senha=segredo"})
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** Mock: a coleta real faria requisições às fontes. */
    @MockitoBean
    private ColetaService coletaService;

    /** Clients falsos garantem que os testes do endpoint nunca consultem os ATSs. */
    @MockitoBean
    private GreenhouseApiClient greenhouseApiClient;

    @MockitoBean
    private LeverApiClient leverApiClient;

    /** Cliente falso: nenhum teste administrativo chama a API real do Telegram. */
    @MockitoBean
    private ClienteTelegram clienteTelegram;

    @Autowired
    private HistoricoColetaService historicoColetaService;

    @Autowired
    private ExecucaoColetaRepository execucaoRepository;

    @Autowired
    private ExecucaoFonteRepository execucaoFonteRepository;

    @Autowired
    private AndamentoColeta andamentoColeta;

    @Autowired
    private PausaAgendamento pausaAgendamento;

    @Autowired
    private ConfiguracaoRepository configuracaoRepository;

    @Autowired
    private VagaDescartadaRepository vagaDescartadaRepository;

    @Autowired
    private DescarteService descarteService;

    @BeforeEach
    void limparConfiguracao() {
        configuracaoRepository.deleteAll();
        vagaDescartadaRepository.deleteAll();
    }

    @Test
    void apiPublica_deveContinuarAbertaSemCredencial() throws Exception {
        mockMvc.perform(get("/api/vagas")).andExpect(status().isOk());
        mockMvc.perform(get("/api/vagas/contagens")).andExpect(status().isOk());
    }

    @Test
    void me_semCredencial_deveResponder401SemAbrirOPopupDoNavegador() throws Exception {
        mockMvc.perform(get("/api/admin/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void me_credencialErrada_deveResponder401() throws Exception {
        mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, basic("admin", "errada")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, basic("outro", "segredo")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_credencialCerta_deveDevolverOUsuarioSemCriarSessao() throws Exception {
        mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, basic("admin", "segredo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("admin"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void coletas_semCredencial_naoDeveRodarAColeta() throws Exception {
        mockMvc.perform(post("/api/admin/coletas"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/coletas/GUPY"))
                .andExpect(status().isUnauthorized());
        verify(coletaService, never()).disparar(any());
        verify(coletaService, never()).disparar(any(), any());
    }

    @Test
    void coletas_credencialCerta_deveDispararEmSegundoPlanoMesmoSemTokenCsrf() throws Exception {
        when(coletaService.disparar(OrigemColeta.MANUAL)).thenReturn(ColetaService.Disparo.INICIADA);

        mockMvc.perform(post("/api/admin/coletas").header(HttpHeaders.AUTHORIZATION, basic("admin", "segredo")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensagem").value("Coleta iniciada"));
    }

    @Test
    void coletas_comColetaRodando_deveResponder409() throws Exception {
        when(coletaService.disparar(OrigemColeta.MANUAL)).thenReturn(ColetaService.Disparo.JA_EM_ANDAMENTO);

        mockMvc.perform(post("/api/admin/coletas").header(HttpHeaders.AUTHORIZATION, basic("admin", "segredo")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Já existe uma coleta em andamento"));
    }

    @Test
    void coletaDeUmaFonte_deveResponderConformeODisparo() throws Exception {
        when(coletaService.disparar(OrigemColeta.MANUAL, "GUPY")).thenReturn(ColetaService.Disparo.INICIADA);
        when(coletaService.disparar(OrigemColeta.MANUAL, "LINKEDIN")).thenReturn(ColetaService.Disparo.FONTE_DESLIGADA);
        when(coletaService.disparar(OrigemColeta.MANUAL, "NADA")).thenReturn(ColetaService.Disparo.FONTE_INEXISTENTE);
        when(coletaService.disparar(OrigemColeta.MANUAL, "LEVER")).thenReturn(ColetaService.Disparo.JA_EM_ANDAMENTO);
        String credencial = basic("admin", "segredo");

        mockMvc.perform(post("/api/admin/coletas/GUPY").header(HttpHeaders.AUTHORIZATION, credencial))
                .andExpect(status().isAccepted());
        mockMvc.perform(post("/api/admin/coletas/LINKEDIN").header(HttpHeaders.AUTHORIZATION, credencial))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("A fonte LINKEDIN está desligada"));
        mockMvc.perform(post("/api/admin/coletas/NADA").header(HttpHeaders.AUTHORIZATION, credencial))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Fonte desconhecida: NADA"));
        mockMvc.perform(post("/api/admin/coletas/LEVER").header(HttpHeaders.AUTHORIZATION, credencial))
                .andExpect(status().isConflict());
    }

    @Test
    void agendamento_semCredencial_deveResponder401() throws Exception {
        mockMvc.perform(post("/api/admin/agendamento/pausar")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/agendamento/retomar")).andExpect(status().isUnauthorized());
        assertThat(pausaAgendamento.isPausado()).isFalse();
    }

    @Test
    void agendamento_pausarERetomar_deveAparecerNoPainel() throws Exception {
        String credencial = basic("admin", "segredo");
        try {
            mockMvc.perform(post("/api/admin/agendamento/pausar").header(HttpHeaders.AUTHORIZATION, credencial))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pausado").value(true));
            mockMvc.perform(get("/api/admin/crawlers").header(HttpHeaders.AUTHORIZATION, credencial))
                    .andExpect(jsonPath("$.agendamentoPausado").value(true));

            mockMvc.perform(post("/api/admin/agendamento/retomar").header(HttpHeaders.AUTHORIZATION, credencial))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pausado").value(false));
            mockMvc.perform(get("/api/admin/crawlers").header(HttpHeaders.AUTHORIZATION, credencial))
                    .andExpect(jsonPath("$.agendamentoPausado").value(false));
        } finally {
            pausaAgendamento.retomar();
        }
    }

    @Test
    void execucoes_semCredencial_deveResponder401() throws Exception {
        mockMvc.perform(get("/api/admin/execucoes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void execucoes_credencialCerta_deveDevolverOHistoricoPaginadoComAsFontes() throws Exception {
        execucaoFonteRepository.deleteAll();
        execucaoRepository.deleteAll();
        historicoColetaService.iniciar(OrigemColeta.AGENDADA);
        ExecucaoColeta manual = historicoColetaService.iniciar(OrigemColeta.MANUAL);
        ExecucaoFonte gupy = ExecucaoFonte.sucesso(manual, "GUPY", LocalDateTime.now(), 12, 4, 2);
        historicoColetaService.registrar(gupy);
        historicoColetaService.finalizar(manual, List.of(gupy));

        mockMvc.perform(get("/api/admin/execucoes").param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, basic("admin", "segredo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].origem").value("MANUAL"))
                .andExpect(jsonPath("$.content[0].status").value("SUCESSO"))
                .andExpect(jsonPath("$.content[0].fontes[0].fonte").value("GUPY"))
                .andExpect(jsonPath("$.content[0].fontes[0].encontradas").value(12))
                .andExpect(jsonPath("$.content[0].fontes[0].novas").value(4))
                .andExpect(jsonPath("$.content[0].fontes[0].expiradas").value(2));
    }

    @Test
    void descartes_semCredencial_deveResponder401() throws Exception {
        mockMvc.perform(get("/api/admin/descartes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void descartes_deveFiltrarPaginarEDevolverContagensPorMotivo() throws Exception {
        descarteService.registrar("GUPY", "Desenvolvedor JavaScript Júnior", "Empresa Um", "Remoto",
                "https://empresa.gupy.io/jobs/1", MotivoDescarte.NAO_JAVA);
        descarteService.registrar("LINKEDIN", "Desenvolvedor Java Sênior", "Empresa Dois", "Brasil",
                "https://linkedin.com/jobs/view/2", MotivoDescarte.NIVEL);
        // O mesmo link e motivo atualiza o registro em vez de criar uma duplicata.
        descarteService.registrar("GUPY", "Desenvolvedor JavaScript Jr", "Empresa Um", "Remoto",
                "https://empresa.gupy.io/jobs/1", MotivoDescarte.NAO_JAVA);

        mockMvc.perform(get("/api/admin/descartes")
                        .param("fonte", "gupy")
                        .param("motivo", "NAO_JAVA")
                        .param("busca", "javascript")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, basic("admin", "segredo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descartes.content.length()").value(1))
                .andExpect(jsonPath("$.descartes.totalElements").value(1))
                .andExpect(jsonPath("$.descartes.content[0].fonte").value("GUPY"))
                .andExpect(jsonPath("$.descartes.content[0].titulo").value("Desenvolvedor JavaScript Jr"))
                .andExpect(jsonPath("$.descartes.content[0].motivo").value("NAO_JAVA"))
                .andExpect(jsonPath("$.contagensPorMotivo.NAO_JAVA").value(1))
                .andExpect(jsonPath("$.contagensPorMotivo.NIVEL").value(1))
                .andExpect(jsonPath("$.contagensPorMotivo.NAO_REMOTA").value(0));
    }

    @Test
    void descartes_deveRemoverRegistrosComMaisDeTrintaDias() {
        vagaDescartadaRepository.save(new VagaDescartada(
                "GUPY", "Antiga", "Empresa", "Remoto", "https://gupy.io/jobs/antiga",
                MotivoDescarte.NAO_JAVA, LocalDateTime.now().minusDays(31)));
        vagaDescartadaRepository.save(new VagaDescartada(
                "GUPY", "Recente", "Empresa", "Remoto", "https://gupy.io/jobs/recente",
                MotivoDescarte.NAO_JAVA, LocalDateTime.now().minusDays(29)));

        assertThat(descarteService.removerAntigos()).isEqualTo(1);

        assertThat(vagaDescartadaRepository.findAll())
                .extracting(VagaDescartada::getTitulo)
                .containsExactly("Recente");
    }

    @Test
    void crawlers_semCredencial_deveResponder401() throws Exception {
        mockMvc.perform(get("/api/admin/crawlers"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Nos testes o LinkedIn está desligado e Greenhouse/Lever não têm empresas:
     * todos precisam aparecer mesmo assim, como desligados.
     */
    @Test
    void crawlers_credencialCerta_deveListarTodasAsFontesComSaudeEAndamento() throws Exception {
        execucaoFonteRepository.deleteAll();
        execucaoRepository.deleteAll();
        ExecucaoColeta execucao = historicoColetaService.iniciar(OrigemColeta.AGENDADA);
        LocalDateTime inicio = LocalDateTime.now().minusMinutes(10);
        historicoColetaService.registrar(ExecucaoFonte.sucesso(execucao, "GUPY", inicio, 8, 1, 0));
        historicoColetaService.registrar(ExecucaoFonte.erro(execucao, "GUPY", inicio.plusMinutes(1), "HTML mudou"));
        for (int i = 0; i < 3; i++) {
            historicoColetaService.registrar(ExecucaoFonte.sucesso(execucao, "REMOTEOK", inicio.plusMinutes(i), 0, 0, 0));
        }

        andamentoColeta.iniciar(OrigemColeta.MANUAL);
        andamentoColeta.iniciarFonte("GUPY");
        andamentoColeta.informar("termo 2/6");
        try {
            mockMvc.perform(get("/api/admin/crawlers").header(HttpHeaders.AUTHORIZATION, basic("admin", "segredo")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coletaEmAndamento.origem").value("MANUAL"))
                    .andExpect(jsonPath("$.coletaEmAndamento.fonteAtual").value("GUPY"))
                    .andExpect(jsonPath("$.coletaEmAndamento.progresso").value("termo 2/6"))
                    .andExpect(jsonPath("$.crawlers[*].fonte").value(contains(
                            "GUPY", "PROGRAMATHOR", "REMOTEOK", "LINKEDIN", "GREENHOUSE", "LEVER")))
                    .andExpect(jsonPath("$.crawlers[0].ligada").value(true))
                    .andExpect(jsonPath("$.crawlers[0].rodando").value(true))
                    .andExpect(jsonPath("$.crawlers[0].progresso").value("termo 2/6"))
                    .andExpect(jsonPath("$.crawlers[0].saude").value("FALHA"))
                    .andExpect(jsonPath("$.crawlers[0].ultimaExecucao.status").value("ERRO"))
                    .andExpect(jsonPath("$.crawlers[0].ultimaExecucao.mensagemErro").value("HTML mudou"))
                    .andExpect(jsonPath("$.crawlers[0].ultimoSucesso").isNotEmpty())
                    .andExpect(jsonPath("$.crawlers[1].saude").value("SEM_DADOS"))
                    .andExpect(jsonPath("$.crawlers[1].rodando").value(false))
                    .andExpect(jsonPath("$.crawlers[1].progresso").doesNotExist())
                    .andExpect(jsonPath("$.crawlers[1].ultimaExecucao").doesNotExist())
                    .andExpect(jsonPath("$.crawlers[2].saude").value("ALERTA"))
                    .andExpect(jsonPath("$.crawlers[3].ligada").value(false))
                    .andExpect(jsonPath("$.crawlers[3].saude").value("DESLIGADA"))
                    .andExpect(jsonPath("$.crawlers[4].saude").value("DESLIGADA"))
                    .andExpect(jsonPath("$.crawlers[5].saude").value("DESLIGADA"));
        } finally {
            andamentoColeta.finalizar();
        }
    }

    @Test
    void crawlers_semColetaRodando_naoDeveTerAndamento() throws Exception {
        mockMvc.perform(get("/api/admin/crawlers").header(HttpHeaders.AUTHORIZATION, basic("admin", "segredo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coletaEmAndamento").doesNotExist())
                .andExpect(jsonPath("$.crawlers[0].rodando").value(false))
                // agendamento desligado nos testes
                .andExpect(jsonPath("$.crawlers[0].proximaColeta").doesNotExist());
    }

    @Test
    void coletarPelaApiPublica_naoDeveExistirMais() throws Exception {
        mockMvc.perform(post("/api/vagas/coletar"))
                .andExpect(status().is4xxClientError());
        verify(coletaService, never()).executarTodos(any());
        verify(coletaService, never()).disparar(any());
    }

    @Test
    void cors_preflightDoAdmin_deveSerLiberadoSemCredencial() throws Exception {
        mockMvc.perform(options("/api/admin/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));

        mockMvc.perform(get("/api/vagas").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
    }

    @Test
    void configuracao_semCredencial_deveResponder401() throws Exception {
        mockMvc.perform(get("/api/admin/configuracao")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/admin/configuracao")
                        .contentType("application/json").content(configuracaoJson(30, 7, "java")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void configuracao_deveConsultarPadroesESalvarValoresValidos() throws Exception {
        String credencial = basic("admin", "segredo");
        mockMvc.perform(get("/api/admin/configuracao").header(HttpHeaders.AUTHORIZATION, credencial))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fontesLigadas.GUPY").value(true))
                .andExpect(jsonPath("$.fontesLigadas.LINKEDIN").value(false))
                .andExpect(jsonPath("$.intervaloColetaMinutos").value(360))
                .andExpect(jsonPath("$.diasParaExpirar").value(7));

        mockMvc.perform(put("/api/admin/configuracao")
                        .header(HttpHeaders.AUTHORIZATION, credencial)
                        .contentType("application/json")
                        .content(configuracaoJson(45, 10, "java spring")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervaloColetaMinutos").value(45))
                .andExpect(jsonPath("$.diasParaExpirar").value(10))
                .andExpect(jsonPath("$.termosBuscaGupy[0]").value("java spring"));
    }

    @Test
    void configuracao_invalida_deveResponder400ComMensagemClara() throws Exception {
        mockMvc.perform(put("/api/admin/configuracao")
                        .header(HttpHeaders.AUTHORIZATION, basic("admin", "segredo"))
                        .contentType("application/json")
                        .content(configuracaoJson(20, 7, "java")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O intervalo da coleta deve ser de pelo menos 30 minutos"));
    }

    @Test
    void notificacoesTestar_semCredencial_deveResponder401SemEnviar() throws Exception {
        mockMvc.perform(post("/api/admin/notificacoes/testar"))
                .andExpect(status().isUnauthorized());

        verify(clienteTelegram, never()).enviar(any(), any(), any());
    }

    @Test
    void notificacoesTestar_comCredencial_deveUsarTokenSemDevolveLoNaConfiguracao() throws Exception {
        String credencial = basic("admin", "segredo");
        mockMvc.perform(put("/api/admin/configuracao")
                        .header(HttpHeaders.AUTHORIZATION, credencial)
                        .contentType("application/json")
                        .content(configuracaoTelegramJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificacoesTelegramLigadas").value(true))
                .andExpect(jsonPath("$.telegramTokenPreenchido").value(true))
                .andExpect(jsonPath("$.telegramToken").doesNotExist())
                .andExpect(jsonPath("$.telegramChatId").value("12345"));

        mockMvc.perform(post("/api/admin/notificacoes/testar")
                        .header(HttpHeaders.AUTHORIZATION, credencial))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Mensagem de teste enviada"));

        verify(clienteTelegram).enviar("token-secreto", "12345",
                "Teste de notificações do Dev First Door.");
    }

    @Test
    void testarBoard_semCredencial_deveResponder401SemConsultarOClient() throws Exception {
        mockMvc.perform(post("/api/admin/boards/testar")
                        .contentType("application/json")
                        .content("""
                                { "ats": "GREENHOUSE", "empresa": "acme" }
                                """))
                .andExpect(status().isUnauthorized());

        verify(greenhouseApiClient, never()).buscarBoard(any());
        verify(leverApiClient, never()).buscarBoard(any());
    }

    @Test
    void testarBoard_comCredencial_deveDevolverTotaisEExemplosSemRede() throws Exception {
        GreenhouseJobDto aprovada = new GreenhouseJobDto(
                1L,
                "Junior Software Engineer",
                "Acme",
                new GreenhouseLocationDto("Remote"),
                "https://boards.greenhouse.io/acme/jobs/1",
                "Java e Spring Boot",
                null,
                null
        );
        GreenhouseJobDto reprovada = new GreenhouseJobDto(
                2L,
                "Senior Software Engineer",
                "Acme",
                new GreenhouseLocationDto("Remote"),
                "https://boards.greenhouse.io/acme/jobs/2",
                "Java",
                null,
                null
        );
        when(greenhouseApiClient.buscarBoard("acme"))
                .thenReturn(ConsultaBoard.existente(List.of(aprovada, reprovada)));

        mockMvc.perform(post("/api/admin/boards/testar")
                        .header(HttpHeaders.AUTHORIZATION, basic("admin", "segredo"))
                        .contentType("application/json")
                        .content("""
                                { "ats": "GREENHOUSE", "empresa": "acme" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ats").value("GREENHOUSE"))
                .andExpect(jsonPath("$.empresa").value("acme"))
                .andExpect(jsonPath("$.existe").value(true))
                .andExpect(jsonPath("$.totalVagas").value(2))
                .andExpect(jsonPath("$.totalAprovadas").value(1))
                .andExpect(jsonPath("$.exemplosAprovados[0].titulo").value("Junior Software Engineer"))
                .andExpect(jsonPath("$.exemplosReprovados[0].motivo").value("NIVEL"));
    }

    private static String configuracaoJson(int intervalo, int dias, String termoGupy) {
        return """
                {
                  "fontesLigadas": {
                    "GUPY": true, "PROGRAMATHOR": true, "REMOTEOK": true,
                    "LINKEDIN": false, "GREENHOUSE": true, "LEVER": true
                  },
                  "termosBuscaGupy": ["%s"],
                  "termosBuscaLinkedin": ["estágio java"],
                  "empresasGreenhouse": [],
                  "empresasLever": [],
                  "intervaloColetaMinutos": %d,
                  "diasParaExpirar": %d,
                  "pausaLinkedinMs": 3000,
                  "variacaoPausaLinkedinMs": 2000,
                  "agendamentoPausado": false,
                  "notificacoesTelegramLigadas": false,
                  "telegramToken": null,
                  "telegramChatId": ""
                }
                """.formatted(termoGupy, intervalo, dias);
    }

    private static String configuracaoTelegramJson() {
        return configuracaoJson(360, 7, "java")
                .replace("\"notificacoesTelegramLigadas\": false",
                        "\"notificacoesTelegramLigadas\": true")
                .replace("\"telegramToken\": null", "\"telegramToken\": \"token-secreto\"")
                .replace("\"telegramChatId\": \"\"", "\"telegramChatId\": \"12345\"");
    }

    static String basic(String usuario, String senha) {
        String credenciais = usuario + ":" + senha;
        return "Basic " + Base64.getEncoder().encodeToString(credenciais.getBytes(StandardCharsets.UTF_8));
    }
}
