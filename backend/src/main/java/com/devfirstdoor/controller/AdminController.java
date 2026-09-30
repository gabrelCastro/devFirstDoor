package com.devfirstdoor.controller;

import com.devfirstdoor.controller.dto.ExecucaoColetaResponse;
import com.devfirstdoor.controller.dto.PainelCrawlersResponse;
import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.service.ColetaService;
import com.devfirstdoor.service.EstadoCrawlersService;
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
    private final EstadoCrawlersService estadoCrawlersService;

    public AdminController(ColetaService coletaService, HistoricoColetaService historicoColetaService,
                           EstadoCrawlersService estadoCrawlersService) {
        this.coletaService = coletaService;
        this.historicoColetaService = historicoColetaService;
        this.estadoCrawlersService = estadoCrawlersService;
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

    /**
     * Estado de cada fonte (inclusive as desligadas) e da coleta em andamento. O painel
     * consulta isto por polling.
     */
    @GetMapping("/crawlers")
    public PainelCrawlersResponse crawlers() {
        return estadoCrawlersService.consultar();
    }

    /** Histórico das coletas, mais recentes primeiro, com o resultado de cada fonte. */
    @GetMapping("/execucoes")
    public Page<ExecucaoColetaResponse> execucoes(@PageableDefault(size = 20) Pageable pageable) {
        return historicoColetaService.listar(pageable);
    }
}
