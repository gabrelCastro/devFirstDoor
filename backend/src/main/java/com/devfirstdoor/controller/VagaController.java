package com.devfirstdoor.controller;

import com.devfirstdoor.controller.dto.ContagensResponse;
import com.devfirstdoor.controller.dto.VagaResponse;
import com.devfirstdoor.repository.VagaFiltro;
import com.devfirstdoor.repository.VagaFiltro.Escopo;
import com.devfirstdoor.repository.VagaFiltro.Secao;
import com.devfirstdoor.service.ConsultaVagasService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vagas")
public class VagaController {

    private final ConsultaVagasService consultaVagasService;

    public VagaController(ConsultaVagasService consultaVagasService) {
        this.consultaVagasService = consultaVagasService;
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
}
