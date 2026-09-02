package com.devfirstdoor.crawler.gupy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GupyJobsPageDto(List<GupyJobDto> data, GupyPaginacaoDto pagination) {
}
