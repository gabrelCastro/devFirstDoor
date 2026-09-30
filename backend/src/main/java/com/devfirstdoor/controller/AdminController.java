package com.devfirstdoor.controller;

import com.devfirstdoor.service.ColetaService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ColetaService coletaService;

    public AdminController(ColetaService coletaService) {
        this.coletaService = coletaService;
    }

    /** Usado pelo frontend para validar o login. */
    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        return Map.of("usuario", authentication.getName());
    }

    /** Roda uma coleta agora e devolve quantas vagas novas cada fonte salvou (-1 = fonte falhou). */
    @PostMapping("/coletas")
    public Map<String, Integer> coletarAgora() {
        return coletaService.executarTodos();
    }
}
