package com.devfirstdoor.curriculo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TermosTest {

    @Test
    void deveEncontrarTecnologiasPorSinonimoEComBordaDePalavra() {
        assertThat(Termos.encontrar("Desenvolvi APIs REST com Spring Boot e Postgres, deploy no Docker"))
                .contains("api rest", "spring boot", "postgresql", "docker")
                .doesNotContain("spring");
        assertThat(Termos.encontrar("Projeto em Spring MVC e Spring Boot")).contains("spring", "spring boot");
        assertThat(Termos.encontrar("Administração restante de JavaScript")).containsExactly("javascript");
        assertThat(Termos.encontrar("Aplicação em Node.js e C#")).contains("node.js", "c#");
    }

    @Test
    void canonicoEContem() {
        assertThat(Termos.canonico("Postgres")).isEqualTo("postgresql");
        assertThat(Termos.canonico("React.js")).isEqualTo("react");
        assertThat(Termos.canonico("Figma")).isEqualTo("figma");
        assertThat(Termos.contem("Usei PostgreSQL no backend", "postgres")).isTrue();
        assertThat(Termos.contem("Design no Figma", "figma")).isTrue();
        assertThat(Termos.contem("Configurei o Figmaster", "figma")).isFalse();
    }
}
