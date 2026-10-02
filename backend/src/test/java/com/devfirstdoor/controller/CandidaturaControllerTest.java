package com.devfirstdoor.controller;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.StatusVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.UsuarioRepository;
import com.devfirstdoor.repository.VagaRepository;
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
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.admin.usuario=admin", "app.admin.senha=segredo"})
@AutoConfigureMockMvc
class CandidaturaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private VagaRepository vagaRepository;

    private String joao;
    private String maria;
    private Vaga vaga;

    @BeforeEach
    void preparar() throws Exception {
        usuarioRepository.deleteAll();
        vagaRepository.deleteAll();
        usuarioService.sincronizarAdminInicial();
        joao = contaComToken("joao");
        maria = contaComToken("maria");
        vaga = salvarVaga("Estágio em Java", "Açaí Tech", "Remoto");
    }

    @Test
    void semLogin_deveResponder401() throws Exception {
        mockMvc.perform(get("/api/candidaturas")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/candidaturas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"vagaId\":" + vaga.getId() + "}")).andExpect(status().isUnauthorized());
    }

    @Test
    void acompanharVaga_deveCopiarOsDadosEComecarEmInteresse() throws Exception {
        criar(joao, "{\"vagaId\":" + vaga.getId() + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidatura.vagaId").value(vaga.getId()))
                .andExpect(jsonPath("$.candidatura.vagaStatus").value("ATIVA"))
                .andExpect(jsonPath("$.candidatura.externa").value(false))
                .andExpect(jsonPath("$.candidatura.titulo").value("Estágio em Java"))
                .andExpect(jsonPath("$.candidatura.empresa").value("Açaí Tech"))
                .andExpect(jsonPath("$.candidatura.fonte").value("GUPY"))
                .andExpect(jsonPath("$.candidatura.etapa").value("INTERESSE"))
                .andExpect(jsonPath("$.candidatura.dataCandidatura").isEmpty())
                .andExpect(jsonPath("$.eventos", hasSize(1)))
                .andExpect(jsonPath("$.eventos[0].tipo").value("CRIADA"));

        vaga.alterarStatus(StatusVaga.EXPIRADA);
        vagaRepository.save(vaga);
        mockMvc.perform(get("/api/candidaturas").header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].vagaStatus").value("EXPIRADA"));
    }

    @Test
    void acompanharDuasVezes_deveResponder409ComOIdExistente() throws Exception {
        long id = idDe(criar(joao, "{\"vagaId\":" + vaga.getId() + "}"));

        criar(joao, "{\"vagaId\":" + vaga.getId() + "}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Você já acompanha esta vaga"))
                .andExpect(jsonPath("$.id").value(id));
        // Outra conta pode acompanhar a mesma vaga.
        criar(maria, "{\"vagaId\":" + vaga.getId() + "}").andExpect(status().isCreated());
    }

    @Test
    void vagaInexistenteOuOculta_deveResponder404() throws Exception {
        criar(joao, "{\"vagaId\":999999}").andExpect(status().isNotFound());
        vaga.alterarStatus(StatusVaga.OCULTA);
        vagaRepository.save(vaga);
        criar(joao, "{\"vagaId\":" + vaga.getId() + "}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Vaga não encontrada"));
    }

    @Test
    void externa_deveValidarCamposELink() throws Exception {
        criar(joao, "{\"titulo\":\"Dev Java\",\"empresa\":\"Padaria\",\"link\":\"https://padaria.dev/vaga\","
                + "\"etapa\":\"ENTREVISTA\",\"dataCandidatura\":\"2026-09-01\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidatura.externa").value(true))
                .andExpect(jsonPath("$.candidatura.vagaId").isEmpty())
                .andExpect(jsonPath("$.candidatura.etapa").value("ENTREVISTA"))
                .andExpect(jsonPath("$.candidatura.dataCandidatura").value("2026-09-01"));

        criar(joao, "{\"titulo\":\"Dev Java\",\"empresa\":\" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe a empresa"));
        criar(joao, "{\"titulo\":\"Dev Java\",\"empresa\":\"X\",\"link\":\"javascript:alert(1)\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O link deve começar com http:// ou https://"));
        criar(joao, "{\"titulo\":\"Dev Java\",\"empresa\":\"X\",\"etapa\":\"INEXISTENTE\"}")
                .andExpect(status().isBadRequest());
        String amanha = LocalDate.now().plusDays(1).toString();
        criar(joao, "{\"titulo\":\"Dev Java\",\"empresa\":\"X\",\"dataCandidatura\":\"" + amanha + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void candidaturaDeOutraConta_deveParecerInexistente() throws Exception {
        long id = idDe(criar(joao, "{\"vagaId\":" + vaga.getId() + "}"));

        mockMvc.perform(get("/api/candidaturas/" + id).header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(status().isNotFound());
        atualizar(maria, id, "{\"etapa\":\"OFERTA\"}").andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/candidaturas/" + id).header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/candidaturas").header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void mudarEtapa_deveRegistrarNaLinhaDoTempoEPreencherADataDeEnvio() throws Exception {
        long id = idDe(criar(joao, "{\"vagaId\":" + vaga.getId() + "}"));

        atualizar(joao, id, "{\"etapa\":\"CANDIDATADO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidatura.etapa").value("CANDIDATADO"))
                .andExpect(jsonPath("$.candidatura.dataCandidatura").value(LocalDate.now().toString()));
        atualizar(joao, id, "{\"etapa\":\"ENTREVISTA\",\"dataCandidatura\":\"2026-08-20\"}")
                .andExpect(jsonPath("$.candidatura.dataCandidatura").value("2026-08-20"))
                .andExpect(jsonPath("$.eventos[*].tipo", contains("CRIADA", "ETAPA", "ETAPA")))
                .andExpect(jsonPath("$.eventos[2].etapaAnterior").value("CANDIDATADO"))
                .andExpect(jsonPath("$.eventos[2].etapaNova").value("ENTREVISTA"));

        // Mesma etapa não gera evento novo.
        atualizar(joao, id, "{\"etapa\":\"ENTREVISTA\"}").andExpect(jsonPath("$.eventos", hasSize(3)));
    }

    @Test
    void dadosDeVagaColetada_naoPodemSerEditados() throws Exception {
        long coletada = idDe(criar(joao, "{\"vagaId\":" + vaga.getId() + "}"));
        long externa = idDe(criar(joao, "{\"titulo\":\"Dev\",\"empresa\":\"Padaria\"}"));

        atualizar(joao, coletada, "{\"titulo\":\"Outro\"}").andExpect(status().isBadRequest());
        atualizar(joao, externa, "{\"titulo\":\"Dev Java Jr\",\"local\":\"Recife\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidatura.titulo").value("Dev Java Jr"))
                .andExpect(jsonPath("$.candidatura.empresa").value("Padaria"))
                .andExpect(jsonPath("$.candidatura.local").value("Recife"));
    }

    @Test
    void proximoPasso_deveSerDefinidoELimpo() throws Exception {
        long id = idDe(criar(joao, "{\"vagaId\":" + vaga.getId() + "}"));

        proximoPasso(joao, id, "{\"texto\":\" Entregar teste \",\"data\":\"2026-10-10\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidatura.proximoPasso").value("Entregar teste"))
                .andExpect(jsonPath("$.candidatura.dataProximoPasso").value("2026-10-10"));
        proximoPasso(joao, id, "{\"texto\":\"\",\"data\":\"2026-10-10\"}")
                .andExpect(status().isBadRequest());
        proximoPasso(joao, id, "{\"texto\":null,\"data\":null}")
                .andExpect(jsonPath("$.candidatura.proximoPasso").isEmpty())
                .andExpect(jsonPath("$.candidatura.dataProximoPasso").isEmpty());
    }

    @Test
    void notas_devemEntrarNaLinhaDoTempoESairQuandoApagadas() throws Exception {
        long id = idDe(criar(joao, "{\"vagaId\":" + vaga.getId() + "}"));

        String corpo = mockMvc.perform(post("/api/candidaturas/" + id + "/notas").header(HttpHeaders.AUTHORIZATION, joao)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Recrutadora: Ana\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("NOTA"))
                .andExpect(jsonPath("$.texto").value("Recrutadora: Ana"))
                .andReturn().getResponse().getContentAsString();
        long notaId = ((Number) JsonPath.read(corpo, "$.id")).longValue();

        mockMvc.perform(post("/api/candidaturas/" + id + "/notas").header(HttpHeaders.AUTHORIZATION, joao)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/candidaturas/" + id + "/notas/" + notaId).header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/candidaturas/" + id + "/notas/" + notaId).header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/candidaturas/" + id).header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(jsonPath("$.eventos[*].tipo", contains("CRIADA")));
    }

    @Test
    void excluir_deveRemoverComALinhaDoTempo() throws Exception {
        long id = idDe(criar(joao, "{\"vagaId\":" + vaga.getId() + "}"));
        atualizar(joao, id, "{\"etapa\":\"CANDIDATADO\"}");

        mockMvc.perform(delete("/api/candidaturas/" + id).header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/candidaturas/" + id).header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isNotFound());
        // Pode voltar a acompanhar a mesma vaga.
        criar(joao, "{\"vagaId\":" + vaga.getId() + "}").andExpect(status().isCreated());
    }

    @Test
    void vagaApagada_naoDeveLevarACandidaturaJunto() throws Exception {
        long id = idDe(criar(joao, "{\"vagaId\":" + vaga.getId() + "}"));
        vagaRepository.deleteAll();

        mockMvc.perform(get("/api/candidaturas/" + id).header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidatura.vagaId").isEmpty())
                .andExpect(jsonPath("$.candidatura.externa").value(false))
                .andExpect(jsonPath("$.candidatura.titulo").value("Estágio em Java"));
    }

    @Test
    void resumo_deveContarOFunilDoUsuario() throws Exception {
        long id = idDe(criar(joao, "{\"vagaId\":" + vaga.getId() + "}"));
        atualizar(joao, id, "{\"etapa\":\"CANDIDATADO\"}");
        atualizar(joao, id, "{\"etapa\":\"ENTREVISTA\"}");
        criar(joao, "{\"titulo\":\"Dev\",\"empresa\":\"Padaria\",\"etapa\":\"CANDIDATADO\"}");
        criar(joao, "{\"titulo\":\"Dev\",\"empresa\":\"Mercado\",\"etapa\":\"DESISTENCIA\"}");
        criar(maria, "{\"titulo\":\"Dev\",\"empresa\":\"Outra\",\"etapa\":\"CANDIDATADO\"}");

        mockMvc.perform(get("/api/candidaturas/resumo").header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.ativas").value(2))
                .andExpect(jsonPath("$.encerradas").value(1))
                .andExpect(jsonPath("$.porEtapa.ENTREVISTA").value(1))
                .andExpect(jsonPath("$.porEtapa.CANDIDATADO").value(1))
                .andExpect(jsonPath("$.porEtapa.OFERTA").value(0))
                .andExpect(jsonPath("$.enviadas").value(2))
                .andExpect(jsonPath("$.responderam").value(1))
                .andExpect(jsonPath("$.taxaResposta").value(0.5))
                .andExpect(jsonPath("$.diasMedioAteResposta").value(0.0))
                .andExpect(jsonPath("$.semanas", hasSize(12)))
                .andExpect(jsonPath("$.semanas[11].enviadas").value(2));
    }

    /** Pelo proxy do Vite a requisição chega com o Origin do navegador: PATCH/PUT/DELETE não podem virar 403. */
    @Test
    void cors_deveLiberarTodosOsMetodosDaApi() throws Exception {
        long id = idDe(criar(joao, "{\"vagaId\":" + vaga.getId() + "}"));
        String origem = "http://localhost:5173";

        mockMvc.perform(patch("/api/candidaturas/" + id).header(HttpHeaders.ORIGIN, origem)
                        .header(HttpHeaders.AUTHORIZATION, joao)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"etapa\":\"CANDIDATADO\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/candidaturas/" + id + "/proximo-passo").header(HttpHeaders.ORIGIN, origem)
                        .header(HttpHeaders.AUTHORIZATION, joao)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"x\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/candidaturas/" + id).header(HttpHeaders.ORIGIN, origem)
                        .header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isNoContent());
    }

    @Test
    void exportar_deveGerarCsvParaOExcel() throws Exception {
        criar(joao, "{\"vagaId\":" + vaga.getId() + "}");
        criar(joao, "{\"titulo\":\"=HYPERLINK(\\\"x\\\")\",\"empresa\":\"Padaria \\\"Pão\\\"\"}");

        byte[] csv = mockMvc.perform(get("/api/candidaturas/exportar").header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment; filename=\"candidaturas-")))
                .andReturn().getResponse().getContentAsByteArray();
        String texto = new String(csv, StandardCharsets.UTF_8);

        assertThat(texto).startsWith("﻿Empresa;Vaga;");
        assertThat(texto).contains("\"Açaí Tech\";\"Estágio em Java\";\"Remoto\"");
        assertThat(texto).contains("\"Padaria \"\"Pão\"\"\";\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(texto).contains("\"Externa\";\"Interesse\"");
    }

    private ResultActions criar(String token, String corpo) throws Exception {
        return mockMvc.perform(post("/api/candidaturas").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions atualizar(String token, long id, String corpo) throws Exception {
        return mockMvc.perform(patch("/api/candidaturas/" + id).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions proximoPasso(String token, long id, String corpo) throws Exception {
        return mockMvc.perform(put("/api/candidaturas/" + id + "/proximo-passo").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private long idDe(ResultActions criacao) throws Exception {
        String corpo = criacao.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.candidatura.id")).longValue();
    }

    private String contaComToken(String usuario) throws Exception {
        String credenciais = "{\"usuario\":\"" + usuario + "\",\"senha\":\"senha-forte\"}";
        mockMvc.perform(post("/api/conta").contentType(MediaType.APPLICATION_JSON).content(credenciais))
                .andExpect(status().isCreated());
        String corpo = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credenciais))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(corpo, "$.token");
    }

    private Vaga salvarVaga(String titulo, String empresa, String local) {
        Vaga nova = new Vaga(titulo, empresa, local, NivelVaga.ESTAGIO, "https://example.com/" + titulo.hashCode(),
                "GUPY", null, LocalDateTime.now());
        nova.setHashDeduplicacao(Integer.toHexString((titulo + empresa).hashCode()));
        return vagaRepository.save(nova);
    }
}
