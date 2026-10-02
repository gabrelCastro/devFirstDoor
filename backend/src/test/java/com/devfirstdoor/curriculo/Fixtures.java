package com.devfirstdoor.curriculo;

import com.devfirstdoor.curriculo.PerfilCurriculo.Contato;
import com.devfirstdoor.curriculo.PerfilCurriculo.Curso;
import com.devfirstdoor.curriculo.PerfilCurriculo.Experiencia;
import com.devfirstdoor.curriculo.PerfilCurriculo.Formacao;
import com.devfirstdoor.curriculo.PerfilCurriculo.Idioma;
import com.devfirstdoor.curriculo.PerfilCurriculo.Item;
import com.devfirstdoor.curriculo.PerfilCurriculo.Projeto;
import com.devfirstdoor.curriculo.VagaAnalisada.Requisito;

import java.util.List;

/** Perfil e vaga de exemplo, com ids fixos, compartilhados pelos testes do currículo. */
final class Fixtures {

    private Fixtures() {
    }

    static PerfilCurriculo perfil() {
        return new PerfilCurriculo(
                new Contato("Maria Silva", "maria@exemplo.com", "(81) 99999-0000", "Recife, PE", null,
                        "https://github.com/maria", null),
                "Estudante de ADS apaixonada por backend.",
                List.of(new Experiencia("e-banco", "Estagiária de Desenvolvimento", "Banco X", "Recife", "2025-02", null,
                        List.of(new Item("b-api", "Criei endpoints de cadastro em Java com Spring Boot"),
                                new Item("b-bugs", "Corrigi 15 bugs reportados pelo time de QA")),
                        List.of("Java", "Spring Boot", "PostgreSQL"))),
                List.of(new Projeto("p-agenda", "Agenda", "https://github.com/maria/agenda",
                                List.of(new Item("b-agenda", "App de agenda com React e API em Node.js")),
                                List.of("React", "Node.js")),
                        new Projeto("p-blog", "Blog", null, List.of(new Item("b-blog", "Blog estático em HTML")), List.of("HTML"))),
                List.of(new Formacao("f-ads", "Análise e Desenvolvimento de Sistemas", "UFPE", "2023-02", null, "CURSANDO")),
                List.of(new Curso("c-docker", "Docker para iniciantes", "Alura", "2024")),
                List.of("Git", "Docker", "Inglês técnico"),
                List.of(new Idioma("i-en", "Inglês", "Intermediário")));
    }

    static VagaAnalisada vaga() {
        return new VagaAnalisada("Desenvolvedor Java Júnior", "Empresa Y", "JUNIOR",
                List.of(new Requisito("r1", "Experiência com Java e Spring Boot", List.of("Java", "Spring Boot")),
                        new Requisito("r2", "Conhecimento em Kubernetes", List.of("Kubernetes"))),
                List.of(new Requisito("d1", "Docker", List.of("Docker"))),
                List.of("Desenvolver APIs REST"),
                List.of("Java", "Spring Boot", "APIs REST", "PostgreSQL", "Docker", "Kubernetes"));
    }
}
