package com.devfirstdoor.service;

import com.devfirstdoor.domain.Sessao;
import com.devfirstdoor.domain.Usuario;
import com.devfirstdoor.repository.SessaoRepository;
import com.devfirstdoor.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Cria, valida e revoga tokens de sessão. O token tem 256 bits aleatórios e vai para o cliente
 * uma única vez; o banco guarda só o SHA-256 dele (sem sal: o token já é aleatório o bastante).
 */
@Service
public class SessaoService {

    /** Gravar o último uso a cada requisição seria uma escrita por chamada à API. */
    private static final Duration INTERVALO_REGISTRO_USO = Duration.ofHours(1);

    private final SessaoRepository sessaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Duration duracao;
    private final SecureRandom aleatorio = new SecureRandom();

    public SessaoService(SessaoRepository sessaoRepository, UsuarioRepository usuarioRepository,
                         @Value("${app.sessao.duracao:30d}") Duration duracao) {
        this.sessaoRepository = sessaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.duracao = duracao;
    }

    public record SessaoCriada(String token, Sessao sessao) {
    }

    /** Chamado depois que usuário e senha já foram conferidos. */
    @Transactional
    public SessaoCriada criar(String login, LocalDateTime agora) {
        Usuario usuario = usuarioRepository.findByLogin(UsuarioService.normalizarLogin(login)).orElseThrow();
        sessaoRepository.apagarExpiradas(agora);
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Sessao sessao = sessaoRepository.save(new Sessao(usuario, hash(token), agora, agora.plus(duracao)));
        return new SessaoCriada(token, sessao);
    }

    /** Sessão válida (não expirada, de conta ativa) para o token, já com o usuário carregado. */
    @Transactional
    public Optional<Sessao> validar(String token, LocalDateTime agora) {
        return sessaoRepository.findByTokenHash(hash(token))
                .filter(sessao -> !sessao.isExpirada(agora))
                .filter(sessao -> sessao.getUsuario().isAtivo())
                .map(sessao -> {
                    if (sessao.getUltimoUso().plus(INTERVALO_REGISTRO_USO).isBefore(agora)) {
                        sessao.registrarUso(agora);
                    }
                    return sessao;
                });
    }

    @Transactional
    public void revogar(long sessaoId) {
        sessaoRepository.deleteById(sessaoId);
    }

    /** Revoga todas as sessões do usuário, menos {@code manterSessaoId} (nulo revoga todas). */
    @Transactional
    public void revogarOutras(long usuarioId, Long manterSessaoId) {
        sessaoRepository.revogarOutras(usuarioId, manterSessaoId);
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
