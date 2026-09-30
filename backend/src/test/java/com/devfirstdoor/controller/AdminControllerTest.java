package com.devfirstdoor.controller;

import com.devfirstdoor.service.ColetaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

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
        verify(coletaService, never()).executarTodos();
    }

    @Test
    void coletas_credencialCerta_deveRodarAColetaMesmoSemTokenCsrf() throws Exception {
        when(coletaService.executarTodos()).thenReturn(Map.of("GUPY", 3));

        mockMvc.perform(post("/api/admin/coletas").header(HttpHeaders.AUTHORIZATION, basic("admin", "segredo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.GUPY").value(3));
    }

    @Test
    void coletarPelaApiPublica_naoDeveExistirMais() throws Exception {
        mockMvc.perform(post("/api/vagas/coletar"))
                .andExpect(status().is4xxClientError());
        verify(coletaService, never()).executarTodos();
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
