package com.devfirstdoor.crawler.gupy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GupyPaginacaoDto(Integer total, Integer limit, Integer offset) {
}
