package com.devfirstdoor.controller;

import com.devfirstdoor.controller.dto.ContagensResponse;
import com.devfirstdoor.controller.dto.VagaResponse;
import com.devfirstdoor.repository.VagaFiltro;
import com.devfirstdoor.repository.VagaFiltro.Escopo;
import com.devfirstdoor.repository.VagaFiltro.Secao;
import com.devfirstdoor.service.ColetaService;
import com.devfirstdoor.service.ConsultaVagasService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/vagas")
public class VagaController {

    private final ConsultaVagasService consultaVagasService;
    private final ColetaService coletaService;

    public VagaController(ConsultaVagasService consultaVagasService, ColetaService coletaService) {
        this.consultaVagasService = consultaVagasService;
        this.coletaService = coletaService;
    }

    @GetMapping
    public Page<VagaResponse> listar(@RequestParam(defaultValue = "TODAS") Secao secao,
                                     @RequestParam(defaultValue = "TODAS") Escopo escopo,
                                     @RequestParam(required = false) String q,
                                     @PageableDefault(size = 20) Pageable pageable) {
        return consultaVagasService.listar(new VagaFiltro(secao, escopo, q), pageable)
                .map(VagaResponse::from);
    }

    @GetMapping("/contagens")
    public ContagensResponse contagens(@RequestParam(defaultValue = "TODAS") Secao secao,
                                       @RequestParam(defaultValue = "TODAS") Escopo escopo,
                                       @RequestParam(required = false) String q) {
        return consultaVagasService.contar(new VagaFiltro(secao, escopo, q));
    }

    @PostMapping("/coletar")
    public Map<String, Integer> coletarAgora() {
        return coletaService.executarTodos();
    }
}
