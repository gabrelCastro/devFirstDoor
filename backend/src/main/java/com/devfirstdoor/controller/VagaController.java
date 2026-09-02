package com.devfirstdoor.controller;

import com.devfirstdoor.controller.dto.VagaResponse;
import com.devfirstdoor.repository.VagaRepository;
import com.devfirstdoor.service.ColetaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/vagas")
public class VagaController {

    private final VagaRepository vagaRepository;
    private final ColetaService coletaService;

    public VagaController(VagaRepository vagaRepository, ColetaService coletaService) {
        this.vagaRepository = vagaRepository;
        this.coletaService = coletaService;
    }

    @GetMapping
    public Page<VagaResponse> listar(@PageableDefault(size = 20) Pageable pageable) {
        return vagaRepository.findAllByOrderByDataColetaDesc(pageable)
                .map(VagaResponse::from);
    }

    @PostMapping("/coletar")
    public Map<String, Integer> coletarAgora() {
        return coletaService.executarTodos();
    }
}
