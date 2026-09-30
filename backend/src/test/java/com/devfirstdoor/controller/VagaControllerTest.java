package com.devfirstdoor.controller;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.VagaRepository;
import com.devfirstdoor.service.ConsultaVagasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VagaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VagaRepository vagaRepository;

    @Autowired
    private ConsultaVagasService consultaVagasService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final LocalDateTime agora = LocalDateTime.of(2026, 9, 1, 12, 0);

    @BeforeEach
    void popularBanco() {
        vagaRepository.deleteAll();
        salvar("Estágio em Java", "Açaí Tech", "Remoto", NivelVaga.ESTAGIO, "GUPY", 1);
        salvar("Desenvolvedor Java Júnior", "Banco X", "Remoto", NivelVaga.JUNIOR, "PROGRAMATHOR", 2);
        salvar("Junior Java Developer", "Acme", "Remoto (USA Only)", NivelVaga.JUNIOR, "REMOTEOK", 3);
        salvar("Java Intern", "Globex", "Remoto (Worldwide)", NivelVaga.ESTAGIO, "REMOTEOK", 4);
        salvar("Estagiário Java", "Initech", "Híbrido (São Paulo, SP)", NivelVaga.ESTAGIO, "LINKEDIN", 5);
    }

    @Test
    void listar_semFiltros_devePaginarTodasAsVagasDaMaisRecenteParaAMaisAntiga() throws Exception {
        mockMvc.perform(get("/api/vagas").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].titulo", contains("Estagiário Java", "Java Intern")))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/vagas").param("size", "2").param("page", "2"))
                .andExpect(jsonPath("$.content[*].titulo", contains("Estágio em Java")))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void listar_deveFiltrarPorSecaoEEscopoAntesDaPaginacao() throws Exception {
        mockMvc.perform(get("/api/vagas").param("secao", "REMOTO").param("size", "10"))
                .andExpect(jsonPath("$.content", hasSize(4)));

        mockMvc.perform(get("/api/vagas").param("secao", "ESTAGIO").param("escopo", "NACIONAL"))
                .andExpect(jsonPath("$.content[*].titulo",
                        contains("Estagiário Java", "Java Intern", "Estágio em Java")));

        mockMvc.perform(get("/api/vagas").param("escopo", "GRINGA"))
                .andExpect(jsonPath("$.content[*].titulo", contains("Junior Java Developer")))
                .andExpect(jsonPath("$.content[0].internacional").value(true));
    }

    @Test
    void listar_buscaDeveIgnorarMaiusculasEAcentosEmTituloEmpresaELocal() throws Exception {
        mockMvc.perform(get("/api/vagas").param("q", "ESTÁGI"))
                .andExpect(jsonPath("$.content[*].titulo", contains("Estagiário Java", "Estágio em Java")));

        mockMvc.perform(get("/api/vagas").param("q", "acai"))
                .andExpect(jsonPath("$.content[*].empresa", contains("Açaí Tech")));

        mockMvc.perform(get("/api/vagas").param("q", "sao paulo"))
                .andExpect(jsonPath("$.content[*].empresa", contains("Initech")));

        mockMvc.perform(get("/api/vagas").param("q", "100%"))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void listar_deveRecusarSecaoInvalida() throws Exception {
        mockMvc.perform(get("/api/vagas").param("secao", "QUALQUER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contagens_deveContarCadaAbaRespeitandoOsOutrosFiltros() throws Exception {
        mockMvc.perform(get("/api/vagas/contagens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.fontes", contains("GUPY", "LINKEDIN", "PROGRAMATHOR", "REMOTEOK")))
                .andExpect(jsonPath("$.secao.TODAS").value(5))
                .andExpect(jsonPath("$.secao.REMOTO").value(4))
                .andExpect(jsonPath("$.secao.ESTAGIO").value(3))
                .andExpect(jsonPath("$.escopo.TODAS").value(5))
                .andExpect(jsonPath("$.escopo.NACIONAL").value(4))
                .andExpect(jsonPath("$.escopo.GRINGA").value(1));

        mockMvc.perform(get("/api/vagas/contagens").param("secao", "ESTAGIO").param("escopo", "GRINGA"))
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.secao.TODAS").value(1))
                .andExpect(jsonPath("$.secao.ESTAGIO").value(0))
                .andExpect(jsonPath("$.escopo.NACIONAL").value(3))
                .andExpect(jsonPath("$.escopo.GRINGA").value(0));

        mockMvc.perform(get("/api/vagas/contagens").param("q", "java junior"))
                .andExpect(jsonPath("$.secao.TODAS").value(1))
                .andExpect(jsonPath("$.escopo.NACIONAL").value(1));
    }

    @Test
    void preencherCamposDerivadosPendentes_deveClassificarVagasSalvasAntesDasColunasExistirem() throws Exception {
        jdbcTemplate.update("update vaga set remoto = null, internacional = null, texto_busca = null");

        mockMvc.perform(get("/api/vagas").param("secao", "REMOTO"))
                .andExpect(jsonPath("$.content", hasSize(0)));

        assertThat(consultaVagasService.preencherCamposDerivadosPendentes()).isEqualTo(5);

        mockMvc.perform(get("/api/vagas").param("secao", "REMOTO"))
                .andExpect(jsonPath("$.content", hasSize(4)));
        mockMvc.perform(get("/api/vagas").param("escopo", "GRINGA").param("q", "acme"))
                .andExpect(jsonPath("$.content", hasSize(1)));
        assertThat(consultaVagasService.preencherCamposDerivadosPendentes()).isZero();
    }

    private void salvar(String titulo, String empresa, String local, NivelVaga nivel, String fonte, int horas) {
        Vaga vaga = new Vaga(titulo, empresa, local, nivel, "https://example.com/" + titulo.hashCode(),
                fonte, null, agora.plusHours(horas));
        vaga.setHashDeduplicacao(Integer.toHexString((titulo + empresa).hashCode()));
        vagaRepository.save(vaga);
    }
}
