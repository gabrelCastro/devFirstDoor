package com.devfirstdoor.controller;

import com.devfirstdoor.controller.dto.CadastroRequest;
import com.devfirstdoor.controller.dto.TrocaSenhaRequest;
import com.devfirstdoor.controller.dto.UsuarioResponse;
import com.devfirstdoor.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Conta do próprio usuário. Só o cadastro é público; o resto usa a credencial HTTP Basic. */
@RestController
@RequestMapping("/api/conta")
public class ContaController {

    private final UsuarioService usuarioService;

    public ContaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse cadastrar(@Valid @RequestBody CadastroRequest request) {
        return usuarioService.cadastrar(request.usuario(), request.senha());
    }

    @GetMapping
    public UsuarioResponse me(Authentication authentication) {
        return usuarioService.buscar(authentication.getName());
    }

    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(Authentication authentication, @Valid @RequestBody TrocaSenhaRequest request) {
        usuarioService.trocarSenha(authentication.getName(), request.senhaAtual(), request.novaSenha());
    }
}
