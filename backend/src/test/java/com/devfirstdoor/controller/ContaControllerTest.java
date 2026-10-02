package com.devfirstdoor.controller;

import com.devfirstdoor.domain.Papel;
import com.devfirstdoor.domain.Usuario;
import com.devfirstdoor.repository.UsuarioRepository;
import com.devfirstdoor.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.devfirstdoor.controller.AdminControllerTest.basic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.admin.usuario=admin", "app.admin.senha=segredo"})
@AutoConfigureMockMvc
class ContaControllerTest {

    private static final String ADMIN = basic("admin", "segredo");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @BeforeEach
    void somenteOAdminDaConfiguracao() {
        usuarioRepository.deleteAll();
        usuarioService.sincronizarAdminInicial();
    }

    @Test
    void adminDaConfiguracao_deveExistirNoBancoComSenhaEmHash() {
        Usuario admin = usuarioRepository.findByLogin("admin").orElseThrow();
        assertThat(admin.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(admin.isAtivo()).isTrue();
        assertThat(admin.getSenhaHash()).startsWith("{bcrypt}").doesNotContain("segredo");
    }

    @Test
    void adminDaConfiguracao_deveVoltarAoEstadoDoEnvAoSincronizar() {
        Usuario admin = usuarioRepository.findByLogin("admin").orElseThrow();
        admin.alterarPapel(Papel.USUARIO);
        admin.alterarAtivo(false);
        admin.trocarSenha("{noop}outra");
        usuarioRepository.save(admin);

        usuarioService.sincronizarAdminInicial();

        Usuario sincronizado = usuarioRepository.findByLogin("admin").orElseThrow();
        assertThat(sincronizado.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(sincronizado.isAtivo()).isTrue();
        assertThat(sincronizado.getSenhaHash()).startsWith("{bcrypt}");
    }

    @Test
    void cadastro_deveCriarUsuarioComumComLoginEmMinusculas() throws Exception {
        cadastrar("Maria.Silva", "senha-forte")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").value("maria.silva"))
                .andExpect(jsonPath("$.papel").value("USUARIO"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        // O login não diferencia maiúsculas.
        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, basic("MARIA.silva", "senha-forte")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("maria.silva"))
                .andExpect(jsonPath("$.papel").value("USUARIO"));
    }

    @Test
    void cadastro_naoDeveAceitarPapelEnviadoPeloCliente() throws Exception {
        mockMvc.perform(post("/api/conta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"esperto\",\"senha\":\"senha-forte\",\"papel\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("USUARIO"));
    }

    @Test
    void cadastro_loginRepetidoOuDoAdmin_deveResponder409() throws Exception {
        cadastrar("joao", "senha-forte").andExpect(status().isCreated());
        cadastrar("JOAO", "outra-senha")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Este usuário já existe"));
        cadastrar("Admin", "senha-forte").andExpect(status().isConflict());
    }

    @Test
    void cadastro_dadosInvalidos_deveResponder400ComMensagem() throws Exception {
        cadastrar("joao", "curta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A senha deve ter de 8 a 72 caracteres"));
        cadastrar("jo ao", "senha-forte").andExpect(status().isBadRequest());
        cadastrar("ab", "senha-forte").andExpect(status().isBadRequest());
        // 72 caracteres, mas 144 bytes: o BCrypt ignoraria o resto.
        cadastrar("joao", "é".repeat(72))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A senha passa do limite de 72 bytes"));
    }

    @Test
    void conta_semCredencialOuComSenhaErrada_deveResponder401() throws Exception {
        cadastrar("joao", "senha-forte").andExpect(status().isCreated());
        mockMvc.perform(get("/api/conta")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, basic("joao", "errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioComum_naoDeveAcessarAAreaAdmin() throws Exception {
        cadastrar("joao", "senha-forte").andExpect(status().isCreated());
        String joao = basic("joao", "senha-forte");

        mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/coletas").header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isForbidden());
    }

    @Test
    void trocaDeSenha_deveExigirASenhaAtual() throws Exception {
        cadastrar("joao", "senha-forte").andExpect(status().isCreated());

        trocarSenha(basic("joao", "senha-forte"), "errada", "nova-senha-forte")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A senha atual está incorreta"));
        trocarSenha(basic("joao", "senha-forte"), "senha-forte", "nova-senha-forte")
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, basic("joao", "senha-forte")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, basic("joao", "nova-senha-forte")))
                .andExpect(status().isOk());
    }

    @Test
    void admin_deveListarUsuariosSemHashDeSenha() throws Exception {
        cadastrar("zeca", "senha-forte").andExpect(status().isCreated());
        cadastrar("bia", "senha-forte").andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/usuarios").header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].usuario", contains("admin", "bia", "zeca")))
                .andExpect(jsonPath("$.content[0].papel").value("ADMIN"))
                .andExpect(jsonPath("$.content[0].senhaHash").doesNotExist());
    }

    @Test
    void admin_promoverUsuario_deveLiberarAAreaAdmin() throws Exception {
        long id = idDe(cadastrar("joao", "senha-forte"));
        String joao = basic("joao", "senha-forte");

        mockMvc.perform(patch("/api/admin/usuarios/" + id).header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"papel\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("ADMIN"));

        mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("joao"));
    }

    @Test
    void admin_desativarUsuario_deveBloquearOLogin() throws Exception {
        long id = idDe(cadastrar("joao", "senha-forte"));

        mockMvc.perform(patch("/api/admin/usuarios/" + id).header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(get("/api/conta").header(HttpHeaders.AUTHORIZATION, basic("joao", "senha-forte")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_naoDeveAlterarAPropriaConta() throws Exception {
        long id = usuarioRepository.findByLogin("admin").orElseThrow().getId();

        mockMvc.perform(patch("/api/admin/usuarios/" + id).header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"papel\":\"USUARIO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Não é possível alterar o papel nem desativar a própria conta"));
        assertThat(usuarioRepository.findByLogin("admin").orElseThrow().getPapel()).isEqualTo(Papel.ADMIN);
    }

    @Test
    void admin_usuarioInexistente_deveResponder404() throws Exception {
        mockMvc.perform(patch("/api/admin/usuarios/999999").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Usuário não encontrado"));
    }

    private org.springframework.test.web.servlet.ResultActions cadastrar(String usuario, String senha) throws Exception {
        return mockMvc.perform(post("/api/conta").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"" + usuario + "\",\"senha\":\"" + senha + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions trocarSenha(String credencial, String atual, String nova)
            throws Exception {
        return mockMvc.perform(put("/api/conta/senha").header(HttpHeaders.AUTHORIZATION, credencial)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\":\"" + atual + "\",\"novaSenha\":\"" + nova + "\"}"));
    }

    private long idDe(org.springframework.test.web.servlet.ResultActions cadastro) throws Exception {
        String login = cadastro.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return usuarioRepository.findByLogin(login.replaceAll(".*\"usuario\":\"([^\"]+)\".*", "$1"))
                .orElseThrow().getId();
    }
}
