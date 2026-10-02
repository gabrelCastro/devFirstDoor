package com.devfirstdoor.controller;

import com.devfirstdoor.repository.UsuarioRepository;
import com.devfirstdoor.service.UsuarioService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.admin.usuario=admin", "app.admin.senha=segredo"})
@AutoConfigureMockMvc
class CurriculoPerfilControllerTest {

    static final String PERFIL = """
            {"contato":{"nome":"Maria Silva","email":"maria@exemplo.com","github":"https://github.com/maria"},
             "resumo":"Estudante de ADS",
             "experiencias":[{"cargo":"Estagiária","empresa":"Banco X","inicio":"2025-02",
               "bullets":[{"texto":"Criei APIs REST com Spring Boot"}],"tecnologias":["Java","Spring Boot"]}],
             "projetos":[{"nome":"Agenda","bullets":[{"texto":"App de agenda em React"}],"tecnologias":["React"]}],
             "habilidades":["Git","Docker"]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    private String maria;
    private String joao;

    @BeforeEach
    void preparar() throws Exception {
        usuarioRepository.deleteAll();
        usuarioService.sincronizarAdminInicial();
        maria = token("maria");
        joao = token("joao");
    }

    @Test
    void semLogin_deveResponder401() throws Exception {
        mockMvc.perform(get("/api/curriculo/perfil")).andExpect(status().isUnauthorized());
    }

    @Test
    void contaNova_recebePerfilVazio() throws Exception {
        mockMvc.perform(get("/api/curriculo/perfil").header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experiencias", hasSize(0)))
                .andExpect(jsonPath("$.habilidades", hasSize(0)));
    }

    @Test
    void salvar_deveGerarIdsQueSeMantemNasProximasGravacoes() throws Exception {
        String salvo = mockMvc.perform(put("/api/curriculo/perfil").header(HttpHeaders.AUTHORIZATION, maria)
                        .contentType(MediaType.APPLICATION_JSON).content(PERFIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experiencias[0].id").isString())
                .andExpect(jsonPath("$.experiencias[0].bullets[0].id").isString())
                .andReturn().getResponse().getContentAsString();
        String idExperiencia = JsonPath.read(salvo, "$.experiencias[0].id");

        // Regravar o que voltou mantém os ids.
        mockMvc.perform(put("/api/curriculo/perfil").header(HttpHeaders.AUTHORIZATION, maria)
                        .contentType(MediaType.APPLICATION_JSON).content(salvo))
                .andExpect(jsonPath("$.experiencias[0].id").value(idExperiencia));
        mockMvc.perform(get("/api/curriculo/perfil").header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(jsonPath("$.experiencias[0].id").value(idExperiencia))
                .andExpect(jsonPath("$.contato.nome").value("Maria Silva"));
    }

    @Test
    void perfilDeOutraConta_naoAparece() throws Exception {
        mockMvc.perform(put("/api/curriculo/perfil").header(HttpHeaders.AUTHORIZATION, maria)
                .contentType(MediaType.APPLICATION_JSON).content(PERFIL)).andExpect(status().isOk());
        mockMvc.perform(get("/api/curriculo/perfil").header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(jsonPath("$.experiencias", hasSize(0)));
    }

    @Test
    void dadosInvalidos_respondem400ComMensagem() throws Exception {
        mockMvc.perform(put("/api/curriculo/perfil").header(HttpHeaders.AUTHORIZATION, maria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"experiencias\":[{\"cargo\":\"Dev\",\"empresa\":\"X\",\"inicio\":\"2025/01\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Experiência \"Dev\": início deve estar no formato aaaa-mm"));
    }

    @Test
    void baixar_pdfEDocxComNomeDeArquivoDaPessoa() throws Exception {
        mockMvc.perform(put("/api/curriculo/perfil").header(HttpHeaders.AUTHORIZATION, maria)
                .contentType(MediaType.APPLICATION_JSON).content(PERFIL)).andExpect(status().isOk());

        mockMvc.perform(get("/api/curriculo/perfil/pdf").header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType(MediaType.APPLICATION_PDF))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Maria-Silva-Curriculo.pdf\""));
        mockMvc.perform(get("/api/curriculo/perfil/docx").header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Maria-Silva-Curriculo.docx\""));
        // Sem nome no perfil não há o que baixar.
        mockMvc.perform(get("/api/curriculo/perfil/pdf").header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe seu nome no perfil antes de baixar o currículo."));
        mockMvc.perform(get("/api/curriculo/perfil/exe").header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(status().isNotFound());
    }

    static String token(org.springframework.test.web.servlet.MockMvc mockMvc, String usuario) throws Exception {
        String credenciais = "{\"usuario\":\"" + usuario + "\",\"senha\":\"senha-forte\"}";
        mockMvc.perform(post("/api/conta").contentType(MediaType.APPLICATION_JSON).content(credenciais));
        String corpo = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credenciais))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(corpo, "$.token");
    }

    private String token(String usuario) throws Exception {
        return token(mockMvc, usuario);
    }
}
