package com.devfirstdoor.crawler.linkedin;

import java.time.LocalDate;

/**
 * {@code id} é o identificador numérico da vaga no LinkedIn (null se o card não o expõe),
 * usado para buscar a página de detalhe.
 */
public record LinkedinJobDto(String id, String titulo, String empresa, String local, String link, LocalDate dataPublicacao) {
}
