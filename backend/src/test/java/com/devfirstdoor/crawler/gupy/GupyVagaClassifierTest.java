package com.devfirstdoor.crawler.gupy;

import com.devfirstdoor.crawler.gupy.dto.GupyJobDto;
import com.devfirstdoor.domain.NivelVaga;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GupyVagaClassifierTest {

    private final GupyVagaClassifier classifier = new GupyVagaClassifier(new GupyCrawlerProperties());

    @Test
    void classificarNivel_deveSerEstagioParaQualquerTituloDoTipoEstagio() {
        GupyJobDto job = job("Auxiliar de Marketing", "vacancy_type_internship");

        assertThat(classifier.classificarNivel(job)).contains(NivelVaga.ESTAGIO);
    }

    @Test
    void classificarNivel_deveSerJuniorQuandoEfetivoETituloContemJunior() {
        GupyJobDto job = job("Desenvolvedor(a) Backend Júnior", "vacancy_type_effective");

        assertThat(classifier.classificarNivel(job)).contains(NivelVaga.JUNIOR);
    }

    @Test
    void classificarNivel_deveReconhecerVariacaoJrSemAcento() {
        GupyJobDto job = job("Analista de Dados Jr", "vacancy_type_effective");

        assertThat(classifier.classificarNivel(job)).contains(NivelVaga.JUNIOR);
    }

    @Test
    void classificarNivel_deveSerVazioQuandoEfetivoSemIndicioDeJunior() {
        GupyJobDto job = job("Desenvolvedor Backend Sênior", "vacancy_type_effective");

        assertThat(classifier.classificarNivel(job)).isEqualTo(Optional.empty());
    }

    @Test
    void classificarNivel_deveSerVazioParaAprendizOuBancoDeTalentos() {
        assertThat(classifier.classificarNivel(job("Programa Jovem Aprendiz", "vacancy_type_apprentice"))).isEmpty();
        assertThat(classifier.classificarNivel(job("Banco de Talentos Júnior", "vacancy_type_talent_pool"))).isEmpty();
    }

    @Test
    void isRelevanteParaTech_deveReconhecerPalavrasDeTecnologiaNoTitulo() {
        assertThat(classifier.isRelevanteParaTech(job("Estágio em Desenvolvimento Java", "vacancy_type_internship"))).isTrue();
        assertThat(classifier.isRelevanteParaTech(job("Analista de Sistemas Júnior", "vacancy_type_effective"))).isTrue();
    }

    @Test
    void isRelevanteParaTech_deveDescartarVagasForaDeTecnologia() {
        assertThat(classifier.isRelevanteParaTech(job("Estágio em Recursos Humanos", "vacancy_type_internship"))).isFalse();
        assertThat(classifier.isRelevanteParaTech(job("Auxiliar Administrativo Júnior", "vacancy_type_effective"))).isFalse();
    }

    @Test
    void isRemota_deveRefletirOCampoIsRemoteWorkDaGupy() {
        assertThat(classifier.isRemota(job("Desenvolvedor Júnior Remoto", "vacancy_type_effective", true))).isTrue();
        assertThat(classifier.isRemota(job("Desenvolvedor Júnior Presencial", "vacancy_type_effective", false))).isFalse();
    }

    private GupyJobDto job(String nome, String tipo) {
        return job(nome, tipo, false);
    }

    private GupyJobDto job(String nome, String tipo, boolean remoto) {
        return new GupyJobDto(1L, nome, "Empresa Teste", "São Paulo", "SP", remoto, tipo,
                "2026-09-01T12:00:00.000Z", "https://empresa.gupy.io/job/1");
    }
}
