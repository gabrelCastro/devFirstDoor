package com.devfirstdoor.crawler.gupy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GupyJobDto(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("careerPageName") String careerPageName,
        @JsonProperty("city") String city,
        @JsonProperty("state") String state,
        @JsonProperty("isRemoteWork") Boolean remotoOk,
        @JsonProperty("type") String type,
        @JsonProperty("publishedDate") String publishedDate,
        @JsonProperty("jobUrl") String jobUrl
) {
}
