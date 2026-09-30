package com.devfirstdoor.controller;

import com.devfirstdoor.controller.dto.AtualizacaoVagaAdminRequest;
import com.devfirstdoor.controller.dto.VagaAdminResponse;
import com.devfirstdoor.domain.StatusVaga;
import com.devfirstdoor.service.ModeracaoVagasService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/vagas")
public class AdminVagaController {

    private final ModeracaoVagasService moderacaoVagasService;

    public AdminVagaController(ModeracaoVagasService moderacaoVagasService) {
        this.moderacaoVagasService = moderacaoVagasService;
    }

    @GetMapping
    public Page<VagaAdminResponse> listar(@RequestParam(required = false) StatusVaga status,
                                          @RequestParam(required = false) String fonte,
                                          @RequestParam(required = false) String busca,
                                          @PageableDefault(size = 20) Pageable pageable) {
        return moderacaoVagasService.listar(status, fonte, busca, pageable);
    }

    @PatchMapping("/{id}")
    public VagaAdminResponse atualizar(@PathVariable long id,
                                       @RequestBody AtualizacaoVagaAdminRequest request) {
        return moderacaoVagasService.atualizar(id, request);
    }

    @PostMapping("/reclassificar")
    public Map<String, Integer> reclassificar() {
        return Map.of("reclassificadas", moderacaoVagasService.reclassificarTodas());
    }
}
