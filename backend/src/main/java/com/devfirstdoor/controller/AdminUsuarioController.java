package com.devfirstdoor.controller;

import com.devfirstdoor.controller.dto.AtualizacaoUsuarioAdminRequest;
import com.devfirstdoor.controller.dto.UsuarioResponse;
import com.devfirstdoor.service.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    public AdminUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public Page<UsuarioResponse> listar(@PageableDefault(size = 20) Pageable pageable) {
        return usuarioService.listar(pageable);
    }

    @PatchMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable long id,
                                     @RequestBody AtualizacaoUsuarioAdminRequest request,
                                     Authentication authentication) {
        return usuarioService.atualizarPorAdmin(id, request, authentication.getName());
    }
}
