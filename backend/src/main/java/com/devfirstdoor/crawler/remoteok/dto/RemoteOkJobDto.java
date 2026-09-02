package com.devfirstdoor.crawler.remoteok.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * O primeiro elemento do array retornado por qualquer endpoint da API da RemoteOK
 * é um objeto de metadados/termos de uso (sem id nem position) — filtrado no
 * {@code RemoteOkApiClient} pela ausência desses dois campos.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteOkJobDto(
        @JsonProperty("id") String id,
        @JsonProperty("position") String position,
        @JsonProperty("company") String company,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("location") String location,
        @JsonProperty("url") String url,
        @JsonProperty("date") String date
) {
}
