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

import static org.hamcrest.Matchers.contains;
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
