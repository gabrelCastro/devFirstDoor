package com.devfirstdoor.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.usuarios.cadastro-aberto=false")
@AutoConfigureMockMvc
class CadastroFechadoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void cadastroFechado_deveResponder403() throws Exception {
        mockMvc.perform(post("/api/conta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"joao\",\"senha\":\"senha-forte\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("O cadastro de novas contas está fechado"));
    }
}
