package com.devfirstdoor.curriculo;

import com.devfirstdoor.curriculo.AdaptacaoIa.BlocoIa;
import com.devfirstdoor.curriculo.AdaptacaoIa.BulletIa;
import com.devfirstdoor.curriculo.AdaptacaoIa.ItemIa;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CurriculoFinalTest {

    private static Proposta proposta() {
        return ValidadorAdaptacao.validar(Fixtures.perfil(), Fixtures.vaga(), new AdaptacaoIa(
                new ItemIa("Estudante de ADS focada em Java e Spring Boot.", List.of("e-banco")),
                List.of(new BlocoIa("e-banco", List.of(
                        new BulletIa("Desenvolvi APIs de cadastro em Java e Spring Boot", List.of("b-api"), List.of()),
                        new BulletIa("Implantei tudo em Kubernetes", List.of("b-bugs"), List.of())))),
                List.of(new BlocoIa("p-agenda", List.of(new BulletIa("Construí uma agenda em React", List.of("b-agenda"), List.of())))),
                List.of("Docker"), List.of(), List.of()));
    }

    @Test
    void doPerfil_mantemTudoComoEsta() {
        CurriculoFinal c = CurriculoFinal.doPerfil(Fixtures.perfil());
        assertThat(c.resumo()).isEqualTo("Estudante de ADS apaixonada por backend.");
        assertThat(c.experiencias().get(0).periodo()).isEqualTo("02/2025 – atual");
        assertThat(c.projetos()).hasSize(2);
        assertThat(c.formacoes()).containsExactly("Análise e Desenvolvimento de Sistemas — UFPE (02/2023 – cursando)");
    }

    @Test
    void montar_usaTextosAceitosEVoltaAoOriginalNoBloqueadoOuRecusado() {
        Proposta p = proposta();
        CurriculoFinal tudo = CurriculoFinal.montar(Fixtures.perfil(), null, p, Escolhas.aceitarTudo());
        assertThat(tudo.resumo()).isEqualTo("Estudante de ADS focada em Java e Spring Boot.");
        // O segundo bullet foi bloqueado (Kubernetes): entra o original da fonte.
        assertThat(tudo.experiencias().get(0).bullets())
                .containsExactly("Desenvolvi APIs de cadastro em Java e Spring Boot", "Corrigi 15 bugs reportados pelo time de QA");
        // Projetos: só o escolhido.
        assertThat(tudo.projetos()).extracting(CurriculoFinal.Secao::titulo).containsExactly("Agenda");
        // Habilidades escolhidas primeiro, depois o resto do perfil.
        assertThat(tudo.habilidades()).containsExactly("Docker", "Git", "Inglês técnico");

        CurriculoFinal recusado = CurriculoFinal.montar(Fixtures.perfil(), null, p,
                new Escolhas(false, List.of("e-banco:0")));
        assertThat(recusado.resumo()).isEqualTo("Estudante de ADS apaixonada por backend.");
        assertThat(recusado.experiencias().get(0).bullets())
                .containsExactly("Criei endpoints de cadastro em Java com Spring Boot", "Corrigi 15 bugs reportados pelo time de QA");
    }

    @Test
    void semExperiencia_projetosVemPrimeiro() {
        PerfilCurriculo p = Fixtures.perfil();
        PerfilCurriculo semExp = new PerfilCurriculo(p.contato(), p.resumo(), List.of(), p.projetos(), p.formacoes(),
                p.cursos(), p.habilidades(), p.idiomas());
        assertThat(CurriculoFinal.doPerfil(semExp).projetosPrimeiro()).isTrue();
        assertThat(CurriculoFinal.doPerfil(p).projetosPrimeiro()).isFalse();
    }
}
