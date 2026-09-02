package com.devfirstdoor.crawler.remoteok;

import com.devfirstdoor.crawler.remoteok.dto.RemoteOkJobDto;
import com.devfirstdoor.domain.NivelVaga;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteOkVagaClassifierTest {

    private final RemoteOkVagaClassifier classifier = new RemoteOkVagaClassifier(new RemoteOkCrawlerProperties());

    @Test
    void classificarNivel_deveReconhecerEstagioPeloTitulo() {
        assertThat(classifier.classificarNivel(job("Trainee Software Engineer", List.of("engineer"))))
                .contains(NivelVaga.ESTAGIO);
        assertThat(classifier.classificarNivel(job("Software Engineering Intern", List.of())))
                .contains(NivelVaga.ESTAGIO);
    }

    @Test
    void classificarNivel_deveReconhecerJuniorPeloTitulo() {
        assertThat(classifier.classificarNivel(job("Junior Front End Developer", List.of())))
                .contains(NivelVaga.JUNIOR);
    }

    @Test
    void classificarNivel_naoDeveUsarAsTagsParaClassificar() {
        // Regressão: as tags da RemoteOK vêm genéricas/repetidas (ex: "Room Attendant"
        // marcada com a tag "junior"), então só o título pode indicar o nível.
        RemoteOkJobDto job = job("Room Attendant", List.of("junior", "internship", "engineer", "dev"));

        assertThat(classifier.classificarNivel(job)).isEmpty();
    }

    @Test
    void isRelevanteParaTech_deveUsarSoOTitulo() {
        assertThat(classifier.isRelevanteParaTech(job("Junior Data Analyst", List.of()))).isTrue();
        assertThat(classifier.isRelevanteParaTech(job("Junior Front End Developer", List.of()))).isTrue();
        assertThat(classifier.isRelevanteParaTech(job("Junior Sales Representative", List.of("junior", "sales", "engineer")))).isFalse();
    }

    private RemoteOkJobDto job(String position, List<String> tags) {
        return new RemoteOkJobDto("1", position, "Empresa Teste", tags, "Remote, ",
                "https://remoteok.com/remote-jobs/1", "2026-08-31T21:46:13+00:00");
    }
}
