package com.devfirstdoor.controller;

import com.devfirstdoor.domain.MotivoDescarte;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.StatusVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.domain.VagaDescartada;
import com.devfirstdoor.repository.VagaDescartadaRepository;
import com.devfirstdoor.repository.VagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.admin.usuario=admin", "app.admin.senha=segredo"})
@AutoConfigureMockMvc
class AdminMetricasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VagaRepository vagaRepository;

    @Autowired
    private VagaDescartadaRepository vagaDescartadaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int sequencia;

    @BeforeEach
    void limparBanco() {
        vagaDescartadaRepository.deleteAll();
        vagaRepository.deleteAll();
        sequencia = 0;
    }

    @Test
    void metricas_semCredencial_deveResponder401() throws Exception {
        mockMvc.perform(get("/api/admin/metricas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void metricas_deveAgregarVagasEDescartesNoBanco() throws Exception {
        LocalDate hoje = LocalDate.now();

        salvar("Desenvolvedor JÁVA Júnior", "Açaí Tech", "Remoto", NivelVaga.JUNIOR,
                "GUPY", hoje.minusDays(2).atTime(10, 0), StatusVaga.ATIVA);
        salvar(" desenvolvedor java   júnior ", "ACAI TECH", "São Paulo", NivelVaga.ESTAGIO,
                "LINKEDIN", hoje.minusDays(2).atTime(11, 0), StatusVaga.ATIVA);
        salvar("Estágio Java", "Empresa B", "Remoto", NivelVaga.ESTAGIO,
                "GUPY", hoje.atTime(9, 0), StatusVaga.ATIVA);
        salvar("Java Developer", "Empresa C", "Remoto (Worldwide)", NivelVaga.JUNIOR,
                "REMOTEOK", hoje.minusDays(29).atTime(8, 0), StatusVaga.ATIVA);
        salvar("Vaga antiga", "Empresa D", "Remoto", NivelVaga.JUNIOR,
                "GUPY", hoje.minusDays(30).atTime(8, 0), StatusVaga.ATIVA);
        salvar("Vaga oculta", "Empresa E", "Remoto", NivelVaga.JUNIOR,
                "LEVER", hoje.minusDays(1).atTime(8, 0), StatusVaga.OCULTA);

        Vaga expiradaSeisDias = salvar("Expirada A", "Empresa F", "Remoto", NivelVaga.JUNIOR,
                "GUPY", hoje.minusDays(10).atTime(8, 0), StatusVaga.EXPIRADA);
        Vaga expiradaDoisDias = salvar("Expirada B", "Empresa G", "Remoto", NivelVaga.JUNIOR,
                "LEVER", hoje.minusDays(6).atTime(8, 0), StatusVaga.EXPIRADA);
        jdbcTemplate.update("update vaga set data_ultima_visita = ? where id = ?",
                hoje.minusDays(4).atTime(8, 0), expiradaSeisDias.getId());
        jdbcTemplate.update("update vaga set data_ultima_visita = ? where id = ?",
                hoje.minusDays(4).atTime(8, 0), expiradaDoisDias.getId());

        vagaDescartadaRepository.save(new VagaDescartada("GUPY", "Sem Java", "Empresa", "Remoto",
                "https://example.com/descarte/1", MotivoDescarte.NAO_JAVA,
                hoje.minusDays(6).atTime(1, 0)));
        vagaDescartadaRepository.save(new VagaDescartada("GUPY", "Sênior", "Empresa", "Remoto",
                "https://example.com/descarte/2", MotivoDescarte.NIVEL,
                hoje.minusDays(1).atTime(1, 0)));
        vagaDescartadaRepository.save(new VagaDescartada("GUPY", "Antiga", "Empresa", "Remoto",
                "https://example.com/descarte/3", MotivoDescarte.NAO_JAVA,
                hoje.minusDays(7).atTime(23, 0)));

        mockMvc.perform(get("/api/admin/metricas")
                        .header(HttpHeaders.AUTHORIZATION, credencial()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vagasAtivasPorFonte.GUPY").value(3))
                .andExpect(jsonPath("$.vagasAtivasPorFonte.LINKEDIN").value(1))
                .andExpect(jsonPath("$.vagasAtivasPorFonte.REMOTEOK").value(1))
                .andExpect(jsonPath("$.vagasAtivasPorNivel.ESTAGIO").value(2))
                .andExpect(jsonPath("$.vagasAtivasPorNivel.JUNIOR").value(3))
                .andExpect(jsonPath("$.vagasAtivasPorModalidade.REMOTA").value(4))
                .andExpect(jsonPath("$.vagasAtivasPorModalidade.NAO_REMOTA").value(1))
                .andExpect(jsonPath("$.tempoMedioNoArHoras").value(96.0))
                .andExpect(jsonPath("$.vagasExclusivasPorFonte.GUPY").value(2))
                .andExpect(jsonPath("$.vagasExclusivasPorFonte.REMOTEOK").value(1))
                .andExpect(jsonPath("$.vagasExclusivasPorFonte.LINKEDIN").doesNotExist())
                .andExpect(jsonPath("$.descartesPorMotivoUltimos7Dias.NAO_JAVA").value(1))
                .andExpect(jsonPath("$.descartesPorMotivoUltimos7Dias.NIVEL").value(1))
                .andExpect(jsonPath("$.descartesPorMotivoUltimos7Dias.NAO_REMOTA").value(0))
                .andExpect(jsonPath("$.vagasNovasUltimos30Dias[*].fonte", containsInAnyOrder(
                        "GUPY", "GUPY", "GUPY", "LINKEDIN", "LEVER", "LEVER", "REMOTEOK")));
    }

    private Vaga salvar(String titulo, String empresa, String local, NivelVaga nivel,
                        String fonte, LocalDateTime dataColeta, StatusVaga status) {
        sequencia++;
        Vaga vaga = new Vaga(titulo, empresa, local, nivel,
                "https://example.com/metricas/" + sequencia, fonte, null, dataColeta);
        vaga.setHashDeduplicacao("metricas-" + sequencia);
        vaga.alterarStatus(status);
        return vagaRepository.saveAndFlush(vaga);
    }

    private static String credencial() {
        String valor = Base64.getEncoder().encodeToString("admin:segredo".getBytes(StandardCharsets.UTF_8));
        return "Basic " + valor;
    }
}
