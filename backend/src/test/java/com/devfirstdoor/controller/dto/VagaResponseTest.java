package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class VagaResponseTest {

    @Test
    void isRemoto_deveReconhecerOPrefixoRemotoUsadoPorTodasAsFontes() {
        assertThat(VagaResponse.isRemoto(vaga("GUPY", "Remoto"))).isTrue();
        assertThat(VagaResponse.isRemoto(vaga("LINKEDIN", "Remoto (São Paulo, SP)"))).isTrue();
        assertThat(VagaResponse.isRemoto(vaga("LINKEDIN", "Híbrido (São Paulo, SP)"))).isFalse();
        assertThat(VagaResponse.isRemoto(vaga("LINKEDIN", "São Paulo, SP"))).isFalse();
        assertThat(VagaResponse.isRemoto(vaga("LINKEDIN", null))).isFalse();
    }

    @Test
    void isInternacional_deveReconhecerVagaRemoteOkRestritaAOutroPais() {
        assertThat(VagaResponse.isInternacional(vaga("REMOTEOK", "Remoto (USA Only)"))).isTrue();
        assertThat(VagaResponse.isInternacional(vaga("REMOTEOK", "Remoto (United Kingdom)"))).isTrue();
        assertThat(VagaResponse.isInternacional(vaga("REMOTEOK", "Remoto (Europe)"))).isTrue();
    }

    @Test
    void isInternacional_naoDeveMarcarVagaRemoteOkAbertaOuSemLocalizacao() {
        assertThat(VagaResponse.isInternacional(vaga("REMOTEOK", "Remoto (Worldwide)"))).isFalse();
        assertThat(VagaResponse.isInternacional(vaga("REMOTEOK", "Remoto (Brazil)"))).isFalse();
        assertThat(VagaResponse.isInternacional(vaga("REMOTEOK", "Remoto"))).isFalse();
        assertThat(VagaResponse.isInternacional(vaga("REMOTEOK", null))).isFalse();
    }

    @Test
    void isInternacional_naoDeveDarFalsoPositivoPorSubstringSemBordaDePalavra() {
        // "uk" é uma das palavras curtas (<=3 letras) que exigem borda de palavra —
        // não pode casar dentro de "Duke Energy", por exemplo.
        assertThat(VagaResponse.isInternacional(vaga("REMOTEOK", "Remoto (Duke Energy Campus)"))).isFalse();
    }

    @Test
    void isInternacional_deveIgnorarOutrasFontesMesmoComTextoParecido() {
        // Gupy e ProgramaThor são boards 100% nacionais — a checagem só vale pra RemoteOK.
        assertThat(VagaResponse.isInternacional(vaga("GUPY", "Remoto (São Paulo, São Paulo)"))).isFalse();
        assertThat(VagaResponse.isInternacional(vaga("PROGRAMATHOR", "São Paulo, SP"))).isFalse();
    }

    @Test
    void from_devePropagarAClassificacaoParaOCampoInternacional() {
        assertThat(VagaResponse.from(vaga("REMOTEOK", "Remoto (USA Only)")).internacional()).isTrue();
        assertThat(VagaResponse.from(vaga("REMOTEOK", "Remoto (Worldwide)")).internacional()).isFalse();
    }

    private Vaga vaga(String fonte, String local) {
        return new Vaga("Junior Developer", "Empresa Teste", local, NivelVaga.JUNIOR,
                "https://example.com/vaga/1", fonte, null, LocalDateTime.now());
    }
}
