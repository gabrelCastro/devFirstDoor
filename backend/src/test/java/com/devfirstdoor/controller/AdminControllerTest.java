package com.devfirstdoor.controller;

import com.devfirstdoor.domain.ExecucaoColeta;
import com.devfirstdoor.domain.ExecucaoFonte;
import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.repository.ExecucaoColetaRepository;
import com.devfirstdoor.repository.ExecucaoFonteRepository;
import com.devfirstdoor.service.AndamentoColeta;
import com.devfirstdoor.service.ColetaService;
import com.devfirstdoor.service.HistoricoColetaService;
import com.devfirstdoor.service.PausaAgendamento;
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
    void crawlers_semCredencial_deveResponder401() throws Exception {
        mockMvc.perform(get("/api/admin/crawlers"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Nos testes o LinkedIn está desligado (sem bean) e Greenhouse/Lever não têm empresas:
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

    static String basic(String usuario, String senha) {
        String credenciais = usuario + ":" + senha;
        return "Basic " + Base64.getEncoder().encodeToString(credenciais.getBytes(StandardCharsets.UTF_8));
    }
}
