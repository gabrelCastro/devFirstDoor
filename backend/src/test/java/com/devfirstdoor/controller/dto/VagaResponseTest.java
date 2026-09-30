package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class VagaResponseTest {

    @Test
    void from_devePropagarAClassificacaoParaOCampoInternacional() {
        assertThat(VagaResponse.from(vaga("REMOTEOK", "Remoto (USA Only)")).internacional()).isTrue();
        assertThat(VagaResponse.from(vaga("REMOTEOK", "Remoto (Worldwide)")).internacional()).isFalse();
    }

    @Test
    void from_devePropagarAClassificacaoParaOCampoRemoto() {
        assertThat(VagaResponse.from(vaga("LINKEDIN", "Remoto (São Paulo, SP)")).remoto()).isTrue();
        assertThat(VagaResponse.from(vaga("LINKEDIN", "Híbrido (São Paulo, SP)")).remoto()).isFalse();
    }

    private Vaga vaga(String fonte, String local) {
        return new Vaga("Junior Developer", "Empresa Teste", local, NivelVaga.JUNIOR,
                "https://example.com/vaga/1", fonte, null, LocalDateTime.now());
    }
}
