package com.devfirstdoor.crawler.lever.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LeverCategoriesDto(
        @JsonProperty("location") String location,
        @JsonProperty("commitment") String commitment,
        @JsonProperty("team") String team
) {
}
