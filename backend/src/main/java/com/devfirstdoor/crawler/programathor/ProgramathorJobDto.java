package com.devfirstdoor.crawler.programathor;

import java.util.List;

/** {@code tags} são as tecnologias listadas no card (ex: "Java", "Docker"). */
public record ProgramathorJobDto(String titulo, String empresa, String local, String link, List<String> tags) {
}
