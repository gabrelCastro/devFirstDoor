package com.devfirstdoor.service;

import com.devfirstdoor.config.AdminProperties;
import com.devfirstdoor.controller.dto.AtualizacaoUsuarioAdminRequest;
import com.devfirstdoor.controller.dto.UsuarioResponse;
import com.devfirstdoor.domain.Papel;
import com.devfirstdoor.domain.Usuario;
import com.devfirstdoor.repository.SessaoRepository;
import com.devfirstdoor.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class UsuarioService {

    /** O BCrypt só considera os primeiros 72 bytes; acima disso recusa em vez de truncar. */
    private static final int MAXIMO_BYTES_SENHA = 72;

    private static final Sort ORDEM = Sort.by(Sort.Order.asc("login"));

    private final UsuarioRepository usuarioRepository;
    private final SessaoRepository sessaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;
    private final boolean cadastroAberto;

    public UsuarioService(UsuarioRepository usuarioRepository, SessaoRepository sessaoRepository,
                          PasswordEncoder passwordEncoder,
                          AdminProperties adminProperties,
                          @Value("${app.usuarios.cadastro-aberto:true}") boolean cadastroAberto) {
        this.usuarioRepository = usuarioRepository;
        this.sessaoRepository = sessaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
        this.cadastroAberto = cadastroAberto;
    }

    /** Login sem espaços nas pontas e em minúsculas, para que "Admin" e "admin" sejam a mesma conta. */
    public static String normalizarLogin(String login) {
        return login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
    }

    /** Cadastro público: sempre cria com o papel {@link Papel#USUARIO}. */
    @Transactional
    public UsuarioResponse cadastrar(String usuario, String senha) {
        if (!cadastroAberto) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O cadastro de novas contas está fechado");
        }
        validarTamanhoSenha(senha);
        String login = normalizarLogin(usuario);
        // O login do admin do .env fica reservado mesmo antes de a conta existir.
        if (login.equals(normalizarLogin(adminProperties.getUsuario())) || usuarioRepository.existsByLogin(login)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este usuário já existe");
        }
        try {
            Usuario novo = new Usuario(login, passwordEncoder.encode(senha), Papel.USUARIO, LocalDateTime.now());
            return UsuarioResponse.from(usuarioRepository.saveAndFlush(novo));
        } catch (DataIntegrityViolationException e) {
            // Dois cadastros simultâneos com o mesmo login: o índice único decide.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este usuário já existe");
        }
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(String login) {
        return UsuarioResponse.from(encontrar(login));
    }

    /** Troca a senha e revoga as sessões da conta, menos {@code manterSessaoId} (a que fez o pedido). */
    @Transactional
    public void trocarSenha(String login, String senhaAtual, String novaSenha, Long manterSessaoId) {
        Usuario usuario = encontrar(login);
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha atual está incorreta");
        }
        validarTamanhoSenha(novaSenha);
        usuario.trocarSenha(passwordEncoder.encode(novaSenha));
        sessaoRepository.revogarOutras(usuario.getId(), manterSessaoId);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        PageRequest pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEM);
        return usuarioRepository.findAll(pagina).map(UsuarioResponse::from);
    }

    /**
     * Muda papel e/ou ativação de outra conta. A própria conta fica bloqueada: assim o admin que
     * faz a alteração continua admin ativo e o sistema nunca fica sem nenhum administrador.
     */
    @Transactional
    public UsuarioResponse atualizarPorAdmin(long id, AtualizacaoUsuarioAdminRequest request, String loginAdmin) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        if (usuario.getLogin().equals(normalizarLogin(loginAdmin))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é possível alterar o papel nem desativar a própria conta");
        }
        if (request.papel() != null) {
            usuario.alterarPapel(request.papel());
        }
        if (request.ativo() != null) {
            usuario.alterarAtivo(request.ativo());
        }
        return UsuarioResponse.from(usuario);
    }

    /**
     * Garante que o admin do .env exista, ativo, com papel ADMIN e a senha configurada. O .env é a
     * fonte da verdade dessa conta: trocar ADMIN_PASSWORD e reiniciar troca a senha dela.
     * Sem senha configurada não faz nada (e não desativa contas que já existam).
     */
    @Transactional
    public boolean sincronizarAdminInicial() {
        if (!adminProperties.isHabilitado()) {
            return false;
        }
        String login = normalizarLogin(adminProperties.getUsuario());
        String senha = adminProperties.getSenha();
        validarTamanhoSenha(senha);
        Usuario admin = usuarioRepository.findByLogin(login).orElse(null);
        if (admin == null) {
            usuarioRepository.save(new Usuario(login, passwordEncoder.encode(senha), Papel.ADMIN, LocalDateTime.now()));
            return true;
        }
        if (!passwordEncoder.matches(senha, admin.getSenhaHash())) {
            admin.trocarSenha(passwordEncoder.encode(senha));
            // Senha nova no .env: quem estava logado com a antiga precisa entrar de novo.
            sessaoRepository.revogarOutras(admin.getId(), null);
        }
        admin.alterarPapel(Papel.ADMIN);
        admin.alterarAtivo(true);
        return true;
    }

    private Usuario encontrar(String login) {
        return usuarioRepository.findByLogin(normalizarLogin(login))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    private static void validarTamanhoSenha(String senha) {
        if (senha.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES_SENHA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha passa do limite de 72 bytes");
        }
    }
}
