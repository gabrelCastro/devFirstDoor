package com.devfirstdoor.crawler.greenhouse.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GreenhouseLocationDto(
        @JsonProperty("name") String name
) {
}
