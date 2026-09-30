package com.devfirstdoor.controller;

import com.devfirstdoor.controller.dto.DescartesResponse;
import com.devfirstdoor.domain.MotivoDescarte;
import com.devfirstdoor.service.DescarteService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/descartes")
public class AdminDescarteController {

    private final DescarteService descarteService;

    public AdminDescarteController(DescarteService descarteService) {
        this.descarteService = descarteService;
    }

    @GetMapping
    public DescartesResponse listar(@RequestParam(required = false) String fonte,
                                    @RequestParam(required = false) MotivoDescarte motivo,
                                    @RequestParam(required = false) String busca,
                                    @PageableDefault(size = 20) Pageable pageable) {
        return descarteService.listar(fonte, motivo, busca, pageable);
    }
}
