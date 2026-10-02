package com.devfirstdoor.controller.dto;

/** Descrição da vaga colada pela pessoa; {@code candidaturaId} liga a versão a uma candidatura (opcional). */
public record NovaVersaoCurriculoRequest(String descricaoVaga, Long candidaturaId) {
}
