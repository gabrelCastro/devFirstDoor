package com.devfirstdoor.controller;

import com.devfirstdoor.curriculo.ia.ClienteIa;
import com.devfirstdoor.curriculo.ia.FalhaIaException;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.AnaliseVagaRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.admin.usuario=admin", "app.admin.senha=segredo", "app.ia.limite-diario=2"})
@AutoConfigureMockMvc
class CurriculoVersoesControllerTest {

    static final String DESCRICAO = "Vaga de Desenvolvedor Java Júnior na Empresa Y. Requisitos: experiência com Java e "
            + "Spring Boot, conhecimento em Kubernetes. Desejável: Docker. Você vai desenvolver APIs REST e trabalhar com o time.";

    static final String ANALISE = """
            {"cargo":"Desenvolvedor Java Júnior","empresa":"Empresa Y","nivel":"JUNIOR",
             "obrigatorios":[{"texto":"Experiência com Java e Spring Boot","termos":["Java","Spring Boot"]},
                             {"texto":"Conhecimento em Kubernetes","termos":["Kubernetes"]}],
             "desejaveis":[{"texto":"Docker","termos":["Docker"]}],
             "responsabilidades":["Desenvolver APIs REST"],
             "palavrasChaveAts":["Java","Spring Boot","Kubernetes","Docker"]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AnaliseVagaRepository analiseVagaRepository;

    @Autowired
    private VagaRepository vagaRepository;

    /** IA falsa: nenhum teste chama a OpenAI. */
    @MockitoBean
    private ClienteIa clienteIa;

    private String maria;
    private String joao;
    private String idExperiencia;
    private String idBullet;

    @BeforeEach
    void preparar() throws Exception {
        usuarioRepository.deleteAll();
        analiseVagaRepository.deleteAll();
        usuarioService.sincronizarAdminInicial();
        reset(clienteIa);
        when(clienteIa.configurado()).thenReturn(true);
        maria = CurriculoPerfilControllerTest.token(mockMvc, "maria");
        joao = CurriculoPerfilControllerTest.token(mockMvc, "joao");
        String perfil = mockMvc.perform(put("/api/curriculo/perfil").header(HttpHeaders.AUTHORIZATION, maria)
                        .contentType(MediaType.APPLICATION_JSON).content(CurriculoPerfilControllerTest.PERFIL))
                .andReturn().getResponse().getContentAsString();
        idExperiencia = JsonPath.read(perfil, "$.experiencias[0].id");
        idBullet = JsonPath.read(perfil, "$.experiencias[0].bullets[0].id");
        when(clienteIa.gerarJson(anyString(), anyString(), eq("vaga_analisada"), any())).thenReturn(ANALISE);
        when(clienteIa.gerarJson(anyString(), anyString(), eq("curriculo_adaptado"), any())).thenReturn(adaptacao());
    }

    private String adaptacao() {
        return """
                {"resumo":{"texto":"Estudante de ADS com projetos em Java e Spring Boot.","fontes":["%1$s"]},
                 "experiencias":[{"id":"%1$s","bullets":[
                    {"texto":"Desenvolvi APIs REST com Spring Boot","fontes":["%2$s"],"termosVaga":["Spring Boot"]},
                    {"texto":"Orquestrei containers com Kubernetes","fontes":["%2$s"],"termosVaga":["Kubernetes"]}]}],
                 "projetos":[],
                 "habilidades":["Docker"],
                 "lacunas":[{"requisitoId":"r2","sugestao":"Faça um projeto pequeno com Kubernetes."}],
                 "perguntas":[{"requisitoId":"d1","pergunta":"Você usou Docker no projeto Agenda?"}]}
                """.formatted(idExperiencia, idBullet);
    }

    private ResultActions adaptar(String token, String corpo) throws Exception {
        return mockMvc.perform(post("/api/curriculo/versoes").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static String corpo(String descricao, Long candidaturaId) {
        return tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(
                java.util.Map.of("descricaoVaga", descricao, "candidaturaId", candidaturaId == null ? "" : candidaturaId))
                .replace("\"candidaturaId\":\"\"", "\"candidaturaId\":null");
    }

    @Test
    void adaptar_geraPropostaValidadaComCobertura() throws Exception {
        adaptar(maria, corpo(DESCRICAO, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Desenvolvedor Java Júnior · Empresa Y"))
                .andExpect(jsonPath("$.vaga.obrigatorios[1].id").value("r2"))
                .andExpect(jsonPath("$.proposta.resumo.status").value("OK"))
                .andExpect(jsonPath("$.proposta.experiencias[0].bullets[0].status").value("OK"))
                .andExpect(jsonPath("$.proposta.experiencias[0].bullets[1].status").value("BLOQUEADO"))
                .andExpect(jsonPath("$.proposta.experiencias[0].bullets[1].motivo").value("Cita kubernetes, que não aparece nas fontes deste tópico."))
                .andExpect(jsonPath("$.proposta.lacunas[0].requisito").value("Conhecimento em Kubernetes"))
                .andExpect(jsonPath("$.proposta.perguntas", hasSize(1)))
                .andExpect(jsonPath("$.proposta.cobertura.palavrasChave", hasSize(4)))
                .andExpect(jsonPath("$.proposta.cobertura.depois", containsInAnyOrder("Java", "Spring Boot", "Docker")))
                .andExpect(jsonPath("$.escolhas.usarResumo").value(true));

        mockMvc.perform(get("/api/curriculo/versoes").header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].coberturaDepois").value(3));
        mockMvc.perform(get("/api/curriculo/status").header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(jsonPath("$.iaConfigurada").value(true))
                .andExpect(jsonPath("$.usadasNasUltimas24h").value(1));
    }

    @Test
    void analiseDaVaga_ficaEmCacheEntreAdaptacoes() throws Exception {
        adaptar(maria, corpo(DESCRICAO, null)).andExpect(status().isCreated());
        adaptar(maria, corpo("  " + DESCRICAO.replace(". ", ".\n\n") + "  ", null)).andExpect(status().isCreated());
        verify(clienteIa, times(1)).gerarJson(anyString(), anyString(), eq("vaga_analisada"), any());
        verify(clienteIa, times(2)).gerarJson(anyString(), anyString(), eq("curriculo_adaptado"), any());
    }

    @Test
    void cotaDiaria_respondem429() throws Exception {
        adaptar(maria, corpo(DESCRICAO, null)).andExpect(status().isCreated());
        adaptar(maria, corpo(DESCRICAO, null)).andExpect(status().isCreated());
        adaptar(maria, corpo(DESCRICAO, null))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.mensagem").value("Você usou as 2 adaptações das últimas 24 horas. Tente de novo mais tarde."));
    }

    @Test
    void validacoesAntesDeGastarComIa() throws Exception {
        adaptar(maria, corpo("curta demais", null)).andExpect(status().isBadRequest());
        // Perfil vazio (joao não preencheu).
        adaptar(joao, corpo(DESCRICAO, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Preencha seu perfil com pelo menos uma experiência ou projeto antes de adaptar."));
        when(clienteIa.configurado()).thenReturn(false);
        adaptar(maria, corpo(DESCRICAO, null)).andExpect(status().isServiceUnavailable());
        verify(clienteIa, never()).gerarJson(anyString(), anyString(), anyString(), any());
    }

    @Test
    void falhaDaIa_viraMensagemENaoContaNaCota() throws Exception {
        when(clienteIa.gerarJson(anyString(), anyString(), eq("curriculo_adaptado"), any()))
                .thenThrow(new FalhaIaException(HttpStatus.SERVICE_UNAVAILABLE, "A IA está sobrecarregada."));
        adaptar(maria, corpo(DESCRICAO, null))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem").value("A IA está sobrecarregada."));
        mockMvc.perform(get("/api/curriculo/status").header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(jsonPath("$.usadasNasUltimas24h").value(0));
    }

    @Test
    void escolhas_atualizamCoberturaEVersaoDeOutraContaNaoAparece() throws Exception {
        String criada = adaptar(maria, corpo(DESCRICAO, null)).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(criada, "$.id");
        String chave = JsonPath.read(criada, "$.proposta.experiencias[0].bullets[0].chave");

        mockMvc.perform(put("/api/curriculo/versoes/" + id + "/escolhas").header(HttpHeaders.AUTHORIZATION, maria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usarResumo\":false,\"recusados\":[\"" + chave + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escolhas.usarResumo").value(false))
                .andExpect(jsonPath("$.escolhas.recusados[0]").value(chave));

        mockMvc.perform(get("/api/curriculo/versoes/" + id).header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/curriculo/versoes/" + id).header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/curriculo/versoes/" + id).header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(status().isNoContent());
    }

    @Test
    void versaoLigadaACandidatura_soDaPropriaConta() throws Exception {
        Vaga vaga = new Vaga("Dev Java Jr", "Empresa Y", "Remoto", NivelVaga.JUNIOR, "https://example.com/cv-vaga",
                "GUPY", null, LocalDateTime.now());
        vaga.setHashDeduplicacao("cv-vaga-hash");
        vaga = vagaRepository.save(vaga);
        String cand = mockMvc.perform(post("/api/candidaturas").header(HttpHeaders.AUTHORIZATION, maria)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":" + vaga.getId() + "}"))
                .andReturn().getResponse().getContentAsString();
        long candidaturaId = ((Number) JsonPath.read(cand, "$.candidatura.id")).longValue();

        adaptar(maria, corpo(DESCRICAO, candidaturaId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidaturaId").value(candidaturaId));
        adaptar(joao, corpo(DESCRICAO, candidaturaId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Candidatura não encontrada"));
        vagaRepository.delete(vaga);
    }
}
