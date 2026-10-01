package com.devfirstdoor.controller;

import com.devfirstdoor.controller.dto.MetricasResponse;
import com.devfirstdoor.service.MetricasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/metricas")
public class AdminMetricasController {

    private final MetricasService metricasService;

    public AdminMetricasController(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    @GetMapping
    public MetricasResponse consultar() {
        return metricasService.consultar();
    }
}
