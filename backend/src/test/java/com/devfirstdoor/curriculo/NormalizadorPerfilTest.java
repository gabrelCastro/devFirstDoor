package com.devfirstdoor.curriculo;

import com.devfirstdoor.curriculo.PerfilCurriculo.Contato;
import com.devfirstdoor.curriculo.PerfilCurriculo.Experiencia;
import com.devfirstdoor.curriculo.PerfilCurriculo.Item;
import com.devfirstdoor.curriculo.PerfilCurriculo.Projeto;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormalizadorPerfilTest {

    private static final Contato CONTATO = new Contato(" Maria Silva ", "maria@exemplo.com", null, null, null,
            "https://github.com/maria", null);

    @Test
    void deveLimparTextosGerarIdsEDescartarBulletsVazios() {
        Experiencia exp = new Experiencia(null, " Estagiária ", "Banco X", null, "2025-02", null,
                List.of(new Item(null, " Criei APIs "), new Item(null, "  "), new Item("b-existente", "Testes")),
                List.of("Java", "java", " Spring Boot "));
        PerfilCurriculo perfil = new PerfilCurriculo(CONTATO, "  ", List.of(exp), null, null, null,
                List.of("Git", "GIT", "Docker"), null);

        PerfilCurriculo normalizado = NormalizadorPerfil.normalizar(perfil);

        Experiencia resultado = normalizado.experiencias().get(0);
        assertThat(resultado.id()).startsWith("e-");
        assertThat(resultado.cargo()).isEqualTo("Estagiária");
        assertThat(resultado.bullets()).extracting(Item::texto).containsExactly("Criei APIs", "Testes");
        assertThat(resultado.bullets().get(0).id()).startsWith("b-");
        assertThat(resultado.bullets().get(1).id()).isEqualTo("b-existente");
        assertThat(resultado.tecnologias()).containsExactly("Java", "Spring Boot");
        assertThat(normalizado.habilidades()).containsExactly("Git", "Docker");
        assertThat(normalizado.resumo()).isNull();
        assertThat(normalizado.contato().nome()).isEqualTo("Maria Silva");
        assertThat(normalizado.projetos()).isEmpty();
    }

    @Test
    void idsRepetidosOuInvalidosGanhamIdNovo() {
        Projeto a = new Projeto("p-1", "A", null, List.of(new Item("x", "um")), List.of());
        Projeto b = new Projeto("p-1", "B", null, List.of(new Item("X Inválido!", "dois")), List.of());
        PerfilCurriculo normalizado = NormalizadorPerfil.normalizar(
                new PerfilCurriculo(CONTATO, null, null, List.of(a, b), null, null, null, null));

        assertThat(normalizado.projetos().get(0).id()).isEqualTo("p-1");
        assertThat(normalizado.projetos().get(1).id()).isNotEqualTo("p-1").startsWith("p-");
        assertThat(normalizado.projetos().get(1).bullets().get(0).id()).startsWith("b-");
    }

    @Test
    void deveRecusarDadosInvalidos() {
        assertInvalido(experiencia(null, "Empresa", "2025-01", null), "Informe o cargo de todas as experiências");
        assertInvalido(experiencia("Dev", "Empresa", "2025-13", null), "deve estar no formato aaaa-mm");
        assertInvalido(experiencia("Dev", "Empresa", "2025-05", "2025-01"), "o fim não pode ser antes do início");
        assertThatThrownBy(() -> NormalizadorPerfil.normalizar(new PerfilCurriculo(
                new Contato(null, null, null, null, null, "javascript:alert(1)", null), null, null, null, null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("http");
        assertThatThrownBy(() -> NormalizadorPerfil.normalizar(new PerfilCurriculo(
                new Contato(null, "nao-e-email", null, null, null, null, null), null, null, null, null, null, null, null)))
                .hasMessageContaining("e-mail");
        List<Item> bullets = new ArrayList<>(Collections.nCopies(9, new Item(null, "tópico")));
        assertThatThrownBy(() -> NormalizadorPerfil.normalizar(new PerfilCurriculo(CONTATO, null,
                List.of(new Experiencia(null, "Dev", "X", null, null, null, bullets, null)), null, null, null, null, null)))
                .hasMessageContaining("no máximo 8 tópicos");
    }

    private static void assertInvalido(Experiencia experiencia, String mensagem) {
        assertThatThrownBy(() -> NormalizadorPerfil.normalizar(
                new PerfilCurriculo(CONTATO, null, List.of(experiencia), null, null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(mensagem);
    }

    private static Experiencia experiencia(String cargo, String empresa, String inicio, String fim) {
        return new Experiencia(null, cargo, empresa, null, inicio, fim, List.of(), List.of());
    }
}
