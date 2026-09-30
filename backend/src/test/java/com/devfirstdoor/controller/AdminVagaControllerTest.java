package com.devfirstdoor.controller;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.StatusVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.VagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.admin.usuario=admin", "app.admin.senha=segredo"})
@AutoConfigureMockMvc
class AdminVagaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VagaRepository vagaRepository;

    private Vaga ativa;
    private Vaga expirada;
    private Vaga oculta;

    @BeforeEach
    void popularBanco() {
        vagaRepository.deleteAll();
        ativa = salvar("Desenvolvedor Java Júnior", "Banco A", "Remoto", NivelVaga.JUNIOR, "GUPY", 3);
        expirada = salvar("Estágio Java", "Açaí Tech", "Remoto", NivelVaga.ESTAGIO, "GUPY", 2);
        expirada.alterarStatus(StatusVaga.EXPIRADA);
        oculta = salvar("Junior Java Developer", "Acme", "Remoto (USA Only)",
                NivelVaga.JUNIOR, "REMOTEOK", 1);
        oculta.alterarStatus(StatusVaga.OCULTA);
        vagaRepository.saveAll(java.util.List.of(expirada, oculta));
    }

    @Test
    void endpoints_semCredencial_devemResponder401() throws Exception {
        mockMvc.perform(get("/api/admin/vagas")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/admin/vagas/{id}", ativa.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OCULTA\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/vagas/reclassificar"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/vagas/duplicatas"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/vagas/duplicatas/resolver")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"vagaMantidaId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listar_comCredencial_devePaginarEFiltrarPorStatusFonteEBusca() throws Exception {
        mockMvc.perform(get("/api/admin/vagas")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, credencial()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].titulo",
                        contains("Desenvolvedor Java Júnior", "Estágio Java")))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/admin/vagas")
                        .param("status", "EXPIRADA")
                        .param("fonte", "gupy")
                        .param("busca", "ACAI")
                        .header(HttpHeaders.AUTHORIZATION, credencial()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(expirada.getId()))
                .andExpect(jsonPath("$.content[0].status").value("EXPIRADA"));
    }

    @Test
    void atualizar_comCredencial_deveAlterarStatusNivelERemotoEMarcarCorrecoesManuais() throws Exception {
        mockMvc.perform(patch("/api/admin/vagas/{id}", ativa.getId())
                        .header(HttpHeaders.AUTHORIZATION, credencial())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "OCULTA", "nivel": "ESTAGIO", "remoto": false }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OCULTA"))
                .andExpect(jsonPath("$.nivel").value("ESTAGIO"))
                .andExpect(jsonPath("$.nivelManual").value(true))
                .andExpect(jsonPath("$.remoto").value(false))
                .andExpect(jsonPath("$.remotoManual").value(true));
    }

    @Test
    void reclassificar_comCredencial_devePreservarCorrecoesManuais() throws Exception {
        ativa.corrigirNivel(NivelVaga.ESTAGIO);
        ativa.corrigirRemoto(false);
        vagaRepository.save(ativa);

        mockMvc.perform(post("/api/admin/vagas/reclassificar")
                        .header(HttpHeaders.AUTHORIZATION, credencial()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reclassificadas").value(3));

        mockMvc.perform(get("/api/admin/vagas")
                        .param("busca", "Banco A")
                        .header(HttpHeaders.AUTHORIZATION, credencial()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nivel").value("ESTAGIO"))
                .andExpect(jsonPath("$.content[0].nivelManual").value(true))
                .andExpect(jsonPath("$.content[0].remoto").value(false))
                .andExpect(jsonPath("$.content[0].remotoManual").value(true));
    }

    @Test
    void duplicatas_deveAgruparAtivasDeFontesDiferentesPorTituloEEmpresaNormalizados() throws Exception {
        Vaga gupy = salvar("  Desenvolvedor   JÁVA Júnior ", "Açaí Tech", "Remoto",
                NivelVaga.JUNIOR, "GUPY", 5);
        Vaga linkedin = salvar("desenvolvedor java júnior", "ACAI TECH", "Brasil",
                NivelVaga.JUNIOR, "LINKEDIN", 4);
        salvar("Desenvolvedor java junior ", "Outra Empresa", "Remoto",
                NivelVaga.JUNIOR, "REMOTEOK", 6);
        Vaga expiradaIgual = salvar("DESENVOLVEDOR JAVA JUNIOR", "Acai Tech", "Remoto",
                NivelVaga.JUNIOR, "LEVER", 7);
        expiradaIgual.alterarStatus(StatusVaga.EXPIRADA);
        vagaRepository.save(expiradaIgual);

        mockMvc.perform(get("/api/admin/vagas/duplicatas")
                        .header(HttpHeaders.AUTHORIZATION, credencial()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tituloNormalizado").value("desenvolvedor java junior"))
                .andExpect(jsonPath("$[0].empresaNormalizada").value("acai tech"))
                .andExpect(jsonPath("$[0].vagas[*].id", containsInAnyOrder(
                        gupy.getId().intValue(), linkedin.getId().intValue())))
                .andExpect(jsonPath("$[0].vagas[*].fonte", containsInAnyOrder("GUPY", "LINKEDIN")));
    }

    @Test
    void resolverDuplicatas_deveManterAEscolhidaEOcultarAsDemaisDoGrupo() throws Exception {
        Vaga mantida = salvar("Dev Java Junior", "Empresa X", "Remoto",
                NivelVaga.JUNIOR, "GUPY", 5);
        Vaga ocultada = salvar("DEV JAVA JÚNIOR", " empresa   x ", "Brasil",
                NivelVaga.JUNIOR, "LINKEDIN", 4);

        mockMvc.perform(post("/api/admin/vagas/duplicatas/resolver")
                        .header(HttpHeaders.AUTHORIZATION, credencial())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vagaMantidaId\":" + mantida.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vagaMantidaId").value(mantida.getId()))
                .andExpect(jsonPath("$.ocultadas").value(1));

        assertThat(vagaRepository.findById(mantida.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusVaga.ATIVA);
        assertThat(vagaRepository.findById(ocultada.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusVaga.OCULTA);
        mockMvc.perform(get("/api/admin/vagas/duplicatas")
                        .header(HttpHeaders.AUTHORIZATION, credencial()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private Vaga salvar(String titulo, String empresa, String local, NivelVaga nivel, String fonte, int horas) {
        Vaga vaga = new Vaga(titulo, empresa, local, nivel,
                "https://example.com/admin/" + titulo.hashCode(), fonte, null,
                LocalDateTime.of(2026, 9, 1, 12, 0).plusHours(horas));
        vaga.setHashDeduplicacao(Integer.toHexString((titulo + fonte).hashCode()));
        return vagaRepository.save(vaga);
    }

    private static String credencial() {
        String valor = Base64.getEncoder().encodeToString("admin:segredo".getBytes(StandardCharsets.UTF_8));
        return "Basic " + valor;
    }
}
