package com.devfirstdoor.crawler.greenhouse.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Resposta de GET /v1/boards/{empresa}/jobs: todas as vagas do board, sem paginação. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GreenhouseJobsDto(
        @JsonProperty("jobs") List<GreenhouseJobDto> jobs
) {
}
