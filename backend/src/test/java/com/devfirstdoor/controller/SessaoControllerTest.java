package com.devfirstdoor.controller;

import com.devfirstdoor.domain.Sessao;
import com.devfirstdoor.domain.Usuario;
import com.devfirstdoor.repository.SessaoRepository;
import com.devfirstdoor.repository.UsuarioRepository;
import com.devfirstdoor.service.SessaoService;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.admin.usuario=admin", "app.admin.senha=segredo"})
@AutoConfigureMockMvc
class SessaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SessaoRepository sessaoRepository;

    @Autowired
    private UsuarioService usuarioService;

    @BeforeEach
    void somenteOAdminDaConfiguracao() {
        usuarioRepository.deleteAll();
        usuarioService.sincronizarAdminInicial();
    }

    @Test
    void login_deveDevolverTokenEGuardarSoOHash() throws Exception {
        cadastrar("joao");

        String corpo = login("JOAO", "senha-forte")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiraEm").isString())
                .andExpect(jsonPath("$.usuario.usuario").value("joao"))
                .andExpect(jsonPath("$.usuario.papel").value("USUARIO"))
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(corpo, "$.token");
        assertThat(token).hasSizeGreaterThanOrEqualTo(43);
        assertThat(sessaoRepository.findAll()).singleElement()
                .satisfies(sessao -> assertThat(sessao.getTokenHash()).isEqualTo(SessaoService.hash(token)).isNotEqualTo(token));
    }

    @Test
    void login_falho_deveResponder401SemDizerOMotivo() throws Exception {
        cadastrar("joao");
        login("joao", "errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Usuário ou senha incorretos"));
        login("ninguem", "senha-forte")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Usuário ou senha incorretos"));

        desativar("joao");
        login("joao", "senha-forte")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Usuário ou senha incorretos"));
        assertThat(sessaoRepository.count()).isZero();
    }

    @Test
    void token_deveAutenticarConformeOPapel() throws Exception {
        cadastrar("joao");
        String joao = bearer(entrar("joao", "senha-forte"));
        String admin = bearer(entrar("admin", "segredo"));

        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("joao"));
        mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("admin"));
    }

    @Test
    void token_invalidoOuExpirado_deveResponder401() throws Exception {
        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, "Bearer inventado"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));

        Usuario admin = usuarioRepository.findByLogin("admin").orElseThrow();
        LocalDateTime ontem = LocalDateTime.now().minusDays(1);
        sessaoRepository.save(new Sessao(admin, SessaoService.hash("vencido"), ontem.minusDays(30), ontem));
        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, "Bearer vencido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void desativarOuRebaixar_deveValerNaHoraParaTokensJaEmitidos() throws Exception {
        long id = cadastrar("joao");
        String joao = bearer(entrar("joao", "senha-forte"));
        String admin = bearer(entrar("admin", "segredo"));

        alterar(admin, id, "{\"papel\":\"ADMIN\"}");
        mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, joao)).andExpect(status().isOk());

        alterar(admin, id, "{\"papel\":\"USUARIO\"}");
        mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, joao)).andExpect(status().isForbidden());

        alterar(admin, id, "{\"ativo\":false}");
        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, joao)).andExpect(status().isUnauthorized());
    }

    @Test
    void logout_deveRevogarSoOTokenUsado() throws Exception {
        cadastrar("joao");
        String celular = bearer(entrar("joao", "senha-forte"));
        String notebook = bearer(entrar("joao", "senha-forte"));

        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, celular))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, celular)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, notebook)).andExpect(status().isOk());
    }

    @Test
    void trocaDeSenha_deveEncerrarAsOutrasSessoes() throws Exception {
        cadastrar("joao");
        String atual = bearer(entrar("joao", "senha-forte"));
        String outra = bearer(entrar("joao", "senha-forte"));

        mockMvc.perform(put("/api/conta/senha").header(HttpHeaders.AUTHORIZATION, atual)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"senha-forte\",\"novaSenha\":\"nova-senha-forte\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, atual)).andExpect(status().isOk());
        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, outra)).andExpect(status().isUnauthorized());
    }

    @Test
    void logout_semLogin_deveResponder401() throws Exception {
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }

    private long cadastrar(String usuario) throws Exception {
        String corpo = mockMvc.perform(post("/api/conta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"" + usuario + "\",\"senha\":\"senha-forte\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private ResultActions login(String usuario, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"" + usuario + "\",\"senha\":\"" + senha + "\"}"));
    }

    private String entrar(String usuario, String senha) throws Exception {
        String corpo = login(usuario, senha).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.token");
    }

    private void desativar(String login) {
        Usuario usuario = usuarioRepository.findByLogin(login).orElseThrow();
        usuario.alterarAtivo(false);
        usuarioRepository.save(usuario);
    }

    private void alterar(String admin, long id, String corpo) throws Exception {
        mockMvc.perform(patch("/api/admin/usuarios/" + id).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());
    }

    static String bearer(String token) {
        return "Bearer " + token;
    }
}
