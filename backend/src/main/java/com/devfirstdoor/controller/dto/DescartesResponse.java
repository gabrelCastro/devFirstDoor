package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.MotivoDescarte;
import org.springframework.data.domain.Page;

import java.util.Map;

public record DescartesResponse(
        Page<VagaDescartadaResponse> descartes,
        Map<MotivoDescarte, Long> contagensPorMotivo
) {
}
