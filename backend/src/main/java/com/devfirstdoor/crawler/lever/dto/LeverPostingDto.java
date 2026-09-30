package com.devfirstdoor.crawler.lever.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Vaga da Postings API do Lever. "workplaceType" é "on-site", "remote", "hybrid" ou
 * "unspecified"; "createdAt" vem em milissegundos desde a época.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeverPostingDto(
        @JsonProperty("id") String id,
        @JsonProperty("text") String text,
        @JsonProperty("categories") LeverCategoriesDto categories,
        @JsonProperty("workplaceType") String workplaceType,
        @JsonProperty("createdAt") Long createdAt,
        @JsonProperty("descriptionPlain") String descriptionPlain,
        @JsonProperty("lists") List<LeverListDto> lists,
        @JsonProperty("additionalPlain") String additionalPlain,
        @JsonProperty("hostedUrl") String hostedUrl
) {

    public String local() {
        return categories != null ? categories.location() : null;
    }
}
