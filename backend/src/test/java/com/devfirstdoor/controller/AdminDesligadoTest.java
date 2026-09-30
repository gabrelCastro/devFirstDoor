package com.devfirstdoor.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static com.devfirstdoor.controller.AdminControllerTest.basic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sem {@code ADMIN_PASSWORD} a área admin fica desligada: não existe senha padrão. */
@SpringBootTest(properties = {"app.admin.usuario=admin", "app.admin.senha="})
@AutoConfigureMockMvc
class AdminDesligadoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void semSenhaConfigurada_deveResponder401ComQualquerCredencial() throws Exception {
        mockMvc.perform(get("/api/admin/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, basic("admin", "")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, basic("admin", "admin")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void semSenhaConfigurada_apiPublicaContinuaAberta() throws Exception {
        mockMvc.perform(get("/api/vagas")).andExpect(status().isOk());
    }
}
