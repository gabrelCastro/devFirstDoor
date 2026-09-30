package com.devfirstdoor.crawler.greenhouse.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Vaga da Job Board API do Greenhouse. Com {@code content=true} o campo "content"
 * traz a descrição em HTML com as entidades escapadas (ex: "&amp;lt;p&amp;gt;").
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GreenhouseJobDto(
        @JsonProperty("id") Long id,
        @JsonProperty("title") String title,
        @JsonProperty("company_name") String companyName,
        @JsonProperty("location") GreenhouseLocationDto location,
        @JsonProperty("absolute_url") String absoluteUrl,
        @JsonProperty("content") String content,
        @JsonProperty("first_published") String firstPublished,
        @JsonProperty("updated_at") String updatedAt
) {

    public String nomeDoLocal() {
        return location != null ? location.name() : null;
    }
}
