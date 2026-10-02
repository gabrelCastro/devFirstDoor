package com.devfirstdoor.curriculo;

import com.devfirstdoor.curriculo.ia.Schema;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SchemasIaTest {

    /** Exigência do modo strict: todo objeto fecha propriedades extras e marca todas como obrigatórias. */
    @SuppressWarnings("unchecked")
    private static void assertStrict(Object no) {
        if (no instanceof Map<?, ?> mapa) {
            if ("object".equals(mapa.get("type"))) {
                Map<String, Object> props = (Map<String, Object>) mapa.get("properties");
                assertThat(mapa.get("additionalProperties")).isEqualTo(false);
                assertThat((List<String>) mapa.get("required")).containsExactlyInAnyOrderElementsOf(props.keySet());
            }
            mapa.values().forEach(SchemasIaTest::assertStrict);
        } else if (no instanceof Collection<?> lista) {
            lista.forEach(SchemasIaTest::assertStrict);
        }
    }

    @Test
    void schemasSaoStrict() {
        assertStrict(AnaliseVagaService.schema());
        assertStrict(PromptAdaptacao.schema(Fixtures.perfil(), Fixtures.vaga()));
    }

    @Test
    void schemaDaAdaptacao_trazOsIdsReaisComoEnum() {
        String json = JsonMapper.builder().build().writeValueAsString(PromptAdaptacao.schema(Fixtures.perfil(), Fixtures.vaga()));
        assertThat(json).contains("\"e-banco\"", "\"p-agenda\"", "\"b-api\"", "\"r2\"", "\"Docker\"");
        PerfilCurriculo semProjetos = new PerfilCurriculo(null, null, Fixtures.perfil().experiencias(), List.of(),
                List.of(), List.of(), List.of(), List.of());
        assertThat(JsonMapper.builder().build().writeValueAsString(PromptAdaptacao.schema(semProjetos, Fixtures.vaga())))
                .contains(Schema.SEM_OPCOES);
    }

    @Test
    void entradaDaIa_naoLevaContatoNemLinks() {
        String entrada = PromptAdaptacao.entrada(Fixtures.perfil(), Fixtures.vaga(),
                Casamento.calcular(Fixtures.perfil(), Fixtures.vaga()), JsonMapper.builder().build());
        assertThat(entrada).doesNotContain("maria@exemplo.com", "99999", "Maria Silva", "github.com", "Recife, PE");
        assertThat(entrada).contains("<perfil>", "<vaga>", "<casamento>", "b-api");
        assertThat(entrada.indexOf("<perfil>")).isLessThan(entrada.indexOf("<vaga>"));
    }

    @Test
    void casamento_classificaOsRequisitos() {
        Map<String, Casamento.Evidencia> c = Casamento.calcular(Fixtures.perfil(), Fixtures.vaga());
        assertThat(c.get("r1").status()).isEqualTo(Casamento.Status.TEM);
        assertThat(c.get("r1").evidencias()).contains("b-api", "e-banco");
        assertThat(c.get("r2").status()).isEqualTo(Casamento.Status.NAO_TEM);
        // Docker está no curso: conta como evidência forte.
        assertThat(c.get("d1").status()).isEqualTo(Casamento.Status.TEM);
        assertThat(c.get("d1").evidencias()).contains("c-docker");
    }

    @Test
    void limparAnalise_numeraRequisitosENormaliza() {
        VagaAnalisada bruta = new VagaAnalisada(" Dev Jr ", " ", "MAGO",
                List.of(new VagaAnalisada.Requisito(null, " Java ", List.of("Java", "Java", " ")),
                        new VagaAnalisada.Requisito(null, "  ", List.of())),
                List.of(new VagaAnalisada.Requisito(null, "Docker", List.of("Docker"))),
                null, List.of("Postgres", "PostgreSQL", "Java"));
        VagaAnalisada limpa = AnaliseVagaService.limpar(bruta);
        assertThat(limpa.cargo()).isEqualTo("Dev Jr");
        assertThat(limpa.empresa()).isNull();
        assertThat(limpa.nivel()).isEqualTo("NAO_INFORMADO");
        assertThat(limpa.obrigatorios()).extracting(VagaAnalisada.Requisito::id).containsExactly("r1");
        assertThat(limpa.obrigatorios().get(0).termos()).containsExactly("Java");
        assertThat(limpa.desejaveis().get(0).id()).isEqualTo("d1");
        assertThat(limpa.palavrasChaveAts()).containsExactly("Postgres", "Java");
        assertThat(AnaliseVagaService.hash("a  b\n c")).isEqualTo(AnaliseVagaService.hash(" a b c "));
    }
}
