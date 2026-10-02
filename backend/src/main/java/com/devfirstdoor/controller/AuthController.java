package com.devfirstdoor.controller;

import com.devfirstdoor.config.SessaoAutenticada;
import com.devfirstdoor.controller.dto.LoginRequest;
import com.devfirstdoor.controller.dto.SessaoResponse;
import com.devfirstdoor.service.SessaoService;
import com.devfirstdoor.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** Troca usuário e senha por um token de sessão, e revoga o token no logout. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SessaoService sessaoService;
    private final UsuarioService usuarioService;

    public AuthController(AuthenticationManager authenticationManager, SessaoService sessaoService,
                          UsuarioService usuarioService) {
        this.authenticationManager = authenticationManager;
        this.sessaoService = sessaoService;
        this.usuarioService = usuarioService;
    }

    /** Senha errada, conta inexistente ou desativada: 401 com a mesma mensagem (ver ApiErrosHandler). */
    @PostMapping("/login")
    public SessaoResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication autenticado = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.usuario(), request.senha()));
        var criada = sessaoService.criar(autenticado.getName(), LocalDateTime.now());
        return new SessaoResponse(criada.token(), criada.sessao().getExpiraEm(),
                usuarioService.buscar(autenticado.getName()));
    }

    /** Revoga a sessão do token usado. Com HTTP Basic não há sessão, então não faz nada. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication authentication) {
        if (authentication.getDetails() instanceof SessaoAutenticada sessao) {
            sessaoService.revogar(sessao.sessaoId());
        }
    }
}
