package com.devfirstdoor.crawler.linkedin;

import com.devfirstdoor.domain.NivelVaga;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LinkedinVagaClassifierTest {

    private final LinkedinVagaClassifier classifier = new LinkedinVagaClassifier(new LinkedinCrawlerProperties());

    @Test
    void classificarNivel_deveReconhecerVariacoesDeEstagio() {
        assertThat(classifier.classificarNivel(job("Estágio em Desenvolvimento de Software"))).contains(NivelVaga.ESTAGIO);
        assertThat(classifier.classificarNivel(job("ESTAGIÁRIO DE DESENVOLVIMENTO"))).contains(NivelVaga.ESTAGIO);
        assertThat(classifier.classificarNivel(job("Estagiário(a) em Desenvolvimento de Software"))).contains(NivelVaga.ESTAGIO);
    }

    @Test
    void classificarNivel_deveSerJuniorQuandoTituloContemJunior() {
        assertThat(classifier.classificarNivel(job("Desenvolvedor Backend Júnior"))).contains(NivelVaga.JUNIOR);
        assertThat(classifier.classificarNivel(job("Analista de Sistemas Jr"))).contains(NivelVaga.JUNIOR);
    }

    @Test
    void classificarNivel_deveSerVazioParaPlenoESenior() {
        assertThat(classifier.classificarNivel(job("Desenvolvedor Backend Pleno - Java"))).isEmpty();
        assertThat(classifier.classificarNivel(job("Desenvolvedor Django Sênior - Trabalho Remoto"))).isEmpty();
    }

    @Test
    void isRelevanteParaTech_deveDescartarEstagioForaDeTecnologia() {
        assertThat(classifier.isRelevanteParaTech(job("Estágio em Desenvolvimento Android"))).isTrue();
        assertThat(classifier.isRelevanteParaTech(job("Estágio Administrativo"))).isFalse();
    }

    @Test
    void classificarModalidade_deveLerATextoDaDescricao() {
        assertThat(classifier.classificarModalidade("Desenvolvedor Júnior", "Modelo de trabalho: 100% presencial em Ponta Grossa/PR."))
                .isEqualTo(LinkedinModalidade.PRESENCIAL);
        assertThat(classifier.classificarModalidade("Desenvolvedor Júnior", "Trabalho remoto, de qualquer lugar do Brasil."))
                .isEqualTo(LinkedinModalidade.REMOTO);
        assertThat(classifier.classificarModalidade("Desenvolvedor Júnior", "Modelo híbrido: 3 dias presenciais e 2 remotos."))
                .isEqualTo(LinkedinModalidade.HIBRIDO);
        assertThat(classifier.classificarModalidade("Desenvolvedor Júnior", "Venha fazer parte do nosso time!"))
                .isEqualTo(LinkedinModalidade.DESCONHECIDA);
    }

    @Test
    void classificarModalidade_explicitamente100RemotoVenceMencaoAEncontrosPresenciais() {
        assertThat(classifier.classificarModalidade("Dev Júnior", "Vaga 100% remota, com encontros presenciais trimestrais."))
                .isEqualTo(LinkedinModalidade.REMOTO);
    }

    @Test
    void classificarModalidade_deveConsiderarOTituloQuandoNaoHaDescricao() {
        assertThat(classifier.classificarModalidade("Desenvolvedor Django Júnior - Trabalho Remoto", null))
                .isEqualTo(LinkedinModalidade.REMOTO);
        assertThat(classifier.classificarModalidade("Desenvolvedor (a) Backend Júnior C#/.NET - Híbrido/SP", null))
                .isEqualTo(LinkedinModalidade.HIBRIDO);
    }

    private LinkedinJobDto job(String titulo) {
        return new LinkedinJobDto("1", titulo, "Empresa", "São Paulo, SP", "https://www.linkedin.com/jobs/view/1", null);
    }
}
