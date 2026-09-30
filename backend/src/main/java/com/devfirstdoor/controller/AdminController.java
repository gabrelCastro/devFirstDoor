package com.devfirstdoor.controller;

import com.devfirstdoor.controller.dto.ExecucaoColetaResponse;
import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.service.ColetaService;
import com.devfirstdoor.service.HistoricoColetaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    private final HistoricoColetaService historicoColetaService;

    public AdminController(ColetaService coletaService, HistoricoColetaService historicoColetaService) {
        this.coletaService = coletaService;
        this.historicoColetaService = historicoColetaService;
    }

    /** Usado pelo frontend para validar o login. */
    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        return Map.of("usuario", authentication.getName());
    }

    /** Roda uma coleta agora e devolve quantas vagas novas cada fonte salvou (-1 = fonte falhou). */
    @PostMapping("/coletas")
    public Map<String, Integer> coletarAgora() {
        return coletaService.executarTodos(OrigemColeta.MANUAL);
    }

    /** Histórico das coletas, mais recentes primeiro, com o resultado de cada fonte. */
    @GetMapping("/execucoes")
    public Page<ExecucaoColetaResponse> execucoes(@PageableDefault(size = 20) Pageable pageable) {
        return historicoColetaService.listar(pageable);
    }
}
