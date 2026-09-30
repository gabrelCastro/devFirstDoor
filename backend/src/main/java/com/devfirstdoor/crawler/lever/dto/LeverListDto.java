package com.devfirstdoor.crawler.lever.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Seção da descrição (ex: "Requirements"), com os itens em HTML ("&lt;li&gt;...&lt;/li&gt;"). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeverListDto(
        @JsonProperty("text") String text,
        @JsonProperty("content") String content
) {
}
