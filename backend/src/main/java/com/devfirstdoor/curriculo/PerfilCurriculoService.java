package com.devfirstdoor.curriculo;

import com.devfirstdoor.domain.CurriculoPerfil;
import com.devfirstdoor.domain.Usuario;
import com.devfirstdoor.repository.CurriculoPerfilRepository;
import com.devfirstdoor.repository.UsuarioRepository;
import com.devfirstdoor.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Service
public class PerfilCurriculoService {

    private final CurriculoPerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public PerfilCurriculoService(CurriculoPerfilRepository perfilRepository, UsuarioRepository usuarioRepository,
                                  ObjectMapper objectMapper) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    /** O perfil da conta; uma conta que ainda não preencheu recebe um perfil vazio. */
    @Transactional(readOnly = true)
    public PerfilCurriculo obter(String login) {
        return perfilRepository.findByUsuarioId(usuario(login).getId())
                .map(registro -> objectMapper.readValue(registro.getPerfilJson(), PerfilCurriculo.class))
                .orElseGet(PerfilCurriculo::vazio);
    }

    @Transactional
    public PerfilCurriculo salvar(String login, PerfilCurriculo perfil, LocalDateTime agora) {
        Usuario usuario = usuario(login);
        PerfilCurriculo normalizado = NormalizadorPerfil.normalizar(perfil);
        String json = objectMapper.writeValueAsString(normalizado);
        perfilRepository.findByUsuarioId(usuario.getId()).ifPresentOrElse(
                registro -> registro.atualizar(json, agora),
                () -> perfilRepository.save(new CurriculoPerfil(usuario, json, agora)));
        return normalizado;
    }

    private Usuario usuario(String login) {
        return usuarioRepository.findByLogin(UsuarioService.normalizarLogin(login))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida"));
    }
}
