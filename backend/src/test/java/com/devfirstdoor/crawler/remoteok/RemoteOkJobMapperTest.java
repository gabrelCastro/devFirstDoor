package com.devfirstdoor.crawler.remoteok;

import com.devfirstdoor.crawler.remoteok.dto.RemoteOkJobDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteOkJobMapperTest {

    private final RemoteOkJobMapper mapper = new RemoteOkJobMapper();

    @Test
    void paraVaga_deveMapearCamposBasicos() {
        RemoteOkJobDto job = new RemoteOkJobDto("123", "Junior Front End Developer", "PULSE (MENA)",
                List.of("react", "javascript"), "Cairo, ",
                "https://remoteok.com/remote-jobs/remote-junior-front-end-developer-123", "2026-08-30T10:00:00+00:00");

        Vaga vaga = mapper.paraVaga(job, NivelVaga.JUNIOR);

        assertThat(vaga.getTitulo()).isEqualTo("Junior Front End Developer");
        assertThat(vaga.getEmpresa()).isEqualTo("PULSE (MENA)");
        assertThat(vaga.getLocal()).isEqualTo("Remoto (Cairo)");
        assertThat(vaga.getNivel()).isEqualTo(NivelVaga.JUNIOR);
        assertThat(vaga.getLink()).isEqualTo("https://remoteok.com/remote-jobs/remote-junior-front-end-developer-123");
        assertThat(vaga.getFonte()).isEqualTo("REMOTEOK");
        assertThat(vaga.getDataPublicacao()).isEqualTo(LocalDate.of(2026, 8, 30));
    }

    @Test
    void paraVaga_deveUsarApenasRemotoQuandoLocationEstaVazia() {
        RemoteOkJobDto job = new RemoteOkJobDto("124", "Trainee Software Engineer", "CodeStore",
                List.of(), "", "https://remoteok.com/remote-jobs/124", "2026-08-30T10:00:00+00:00");

        Vaga vaga = mapper.paraVaga(job, NivelVaga.ESTAGIO);

        assertThat(vaga.getLocal()).isEqualTo("Remoto");
    }

    @Test
    void paraVaga_deveTolerarDataDePublicacaoInvalidaOuAusente() {
        RemoteOkJobDto job = new RemoteOkJobDto("125", "Junior Data Analyst", "HiredBuddy",
                List.of(), "", "https://remoteok.com/remote-jobs/125", "data-invalida");

        Vaga vaga = mapper.paraVaga(job, NivelVaga.JUNIOR);

        assertThat(vaga.getDataPublicacao()).isNull();
    }
}
