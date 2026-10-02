package com.devfirstdoor.curriculo;

import com.devfirstdoor.curriculo.AdaptacaoIa.BlocoIa;
import com.devfirstdoor.curriculo.AdaptacaoIa.BulletIa;
import com.devfirstdoor.curriculo.AdaptacaoIa.ItemIa;
import com.devfirstdoor.curriculo.AdaptacaoIa.LacunaIa;
import com.devfirstdoor.curriculo.AdaptacaoIa.PerguntaIa;
import com.devfirstdoor.curriculo.Proposta.Bullet;
import com.devfirstdoor.curriculo.ia.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidadorAdaptacaoTest {

    private static Proposta validar(List<BulletIa> bullets) {
        return ValidadorAdaptacao.validar(Fixtures.perfil(), Fixtures.vaga(), new AdaptacaoIa(
                new ItemIa("Estudante de ADS com experiência em Java e Spring Boot.", List.of("e-banco")),
                List.of(new BlocoIa("e-banco", bullets)), List.of(), List.of(), List.of(), List.of()));
    }

    private static Bullet unico(BulletIa bullet) {
        return validar(List.of(bullet)).experiencias().get(0).bullets().get(0);
    }

    @Test
    void bulletComFonteDoBlocoETecnologiasDasFontes_ficaOk() {
        Bullet b = unico(new BulletIa("Desenvolvi endpoints de cadastro em Java e Spring Boot",
                List.of("b-api"), List.of("Java", "Spring Boot", "Kubernetes")));
        assertThat(b.status()).isEqualTo(Proposta.OK);
        assertThat(b.originais()).containsExactly("Criei endpoints de cadastro em Java com Spring Boot");
        // termosVaga só mantém o que o texto contém.
        assertThat(b.termosVaga()).containsExactly("Java", "Spring Boot");
    }

    @Test
    void conceitoNovoMesmoQuePlausivel_ficaBloqueadoComMotivo() {
        // A fonte diz "endpoints"; "APIs REST" é provável, mas quem confirma é a pessoa, no perfil.
        Bullet b = unico(new BulletIa("Desenvolvi APIs REST de cadastro em Java e Spring Boot", List.of("b-api"), List.of()));
        assertThat(b.status()).isEqualTo(Proposta.BLOQUEADO);
        assertThat(b.motivo()).isEqualTo("Cita api rest, que não aparece nas fontes deste tópico.");
    }

    @Test
    void tecnologiaDaListaDoBloco_podeSerCitadaComOProprioBlocoComoFonte() {
        Bullet b = unico(new BulletIa("Trabalhei com PostgreSQL no backend", List.of("e-banco"), List.of()));
        assertThat(b.status()).isEqualTo(Proposta.OK);
    }

    @Test
    void tecnologiaInventada_ficaBloqueada() {
        Bullet b = unico(new BulletIa("Implantei os serviços com Kubernetes e Docker", List.of("b-api"), List.of()));
        assertThat(b.status()).isEqualTo(Proposta.BLOQUEADO);
        assertThat(b.motivo()).contains("kubernetes").contains("docker");
    }

    @Test
    void numeroInventado_ficaBloqueado_masNumeroDaFontePassa() {
        assertThat(unico(new BulletIa("Corrigi 40 bugs do time de QA", List.of("b-bugs"), List.of())).status())
                .isEqualTo(Proposta.BLOQUEADO);
        assertThat(unico(new BulletIa("Corrigi 15 bugs reportados pelo QA", List.of("b-bugs"), List.of())).status())
                .isEqualTo(Proposta.OK);
    }

    @Test
    void fonteDeOutroBlocoOuInexistente_naoConta() {
        Bullet b = unico(new BulletIa("Criei um app em React", List.of("b-agenda", "inventada"), List.of()));
        assertThat(b.status()).isEqualTo(Proposta.BLOQUEADO);
        assertThat(b.fontes()).isEmpty();
    }

    @Test
    void resumoComTecnologiaForaDoPerfil_ficaBloqueado() {
        Proposta p = ValidadorAdaptacao.validar(Fixtures.perfil(), Fixtures.vaga(), new AdaptacaoIa(
                new ItemIa("Desenvolvedora com Kubernetes em produção.", List.of()),
                List.of(), List.of(), List.of(), List.of(), List.of()));
        assertThat(p.resumo().status()).isEqualTo(Proposta.BLOQUEADO);
        assertThat(p.resumo().original()).isEqualTo("Estudante de ADS apaixonada por backend.");
    }

    @Test
    void blocosHabilidadesLacunasEPerguntas_soComIdsValidos() {
        Proposta p = ValidadorAdaptacao.validar(Fixtures.perfil(), Fixtures.vaga(), new AdaptacaoIa(
                new ItemIa("Estudante de ADS.", List.of()),
                List.of(new BlocoIa("e-inexistente", List.of()), new BlocoIa(Schema.SEM_OPCOES, List.of())),
                List.of(new BlocoIa("p-agenda", List.of(new BulletIa("Construí uma agenda com React", List.of("b-agenda"), List.of())))),
                List.of("docker", "Kubernetes", "java", Schema.SEM_OPCOES),
                List.of(new LacunaIa("r2", "Estude o básico de Kubernetes"), new LacunaIa("r9", "?")),
                List.of(new PerguntaIa("d1", "Você usou Docker em algum projeto?"))));
        assertThat(p.experiencias()).isEmpty();
        assertThat(p.projetos()).hasSize(1);
        assertThat(p.projetos().get(0).titulo()).isEqualTo("Agenda");
        assertThat(p.habilidades()).containsExactly("Docker", "Java");
        assertThat(p.lacunas()).extracting(Proposta.Lacuna::requisito).containsExactly("Conhecimento em Kubernetes");
        assertThat(p.perguntas()).extracting(Proposta.Pergunta::requisito).containsExactly("Docker");
    }

    @Test
    void numerosAusentes_ignoraFormatacao() {
        assertThat(ValidadorAdaptacao.numerosAusentes("Atendi 1.000 usuários em 2024", "1000 usuários desde 2024")).isEmpty();
        assertThat(ValidadorAdaptacao.numerosAusentes("Reduzi 30%", "melhorei o tempo")).containsExactly("30");
    }
}
