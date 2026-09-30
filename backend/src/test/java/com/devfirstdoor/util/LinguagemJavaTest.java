package com.devfirstdoor.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LinguagemJavaTest {

    @Test
    void mencionadaEm_deveReconhecerJavaEmVariasGrafias() {
        assertThat(LinguagemJava.mencionadaEm("Desenvolvedor Java Júnior")).isTrue();
        assertThat(LinguagemJava.mencionadaEm("Conhecimento em JAVA/Spring Boot")).isTrue();
        assertThat(LinguagemJava.mencionadaEm("Experiência com Java8 e JavaEE")).isTrue();
        assertThat(LinguagemJava.mencionadaEm("Stack: Kotlin, java, Docker")).isTrue();
    }

    @Test
    void mencionadaEm_naoDeveConfundirComJavaScript() {
        assertThat(LinguagemJava.mencionadaEm("Desenvolvedor JavaScript Júnior")).isFalse();
        assertThat(LinguagemJava.mencionadaEm("Front-end com javascript e React")).isFalse();
    }

    @Test
    void mencionadaEm_deveAceitarVariosTextosEListas() {
        assertThat(LinguagemJava.mencionadaEm("Desenvolvedor Júnior", null, "Requisitos: Java")).isTrue();
        assertThat(LinguagemJava.mencionadaEm(List.of("Python", "Java"))).isTrue();
        assertThat(LinguagemJava.mencionadaEm(List.of("Python", "Go"))).isFalse();
        assertThat(LinguagemJava.mencionadaEm((List<String>) null)).isFalse();
    }
}
