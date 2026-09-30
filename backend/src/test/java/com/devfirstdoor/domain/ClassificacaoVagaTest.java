package com.devfirstdoor.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificacaoVagaTest {

    @Test
    void isRemoto_deveReconhecerOPrefixoRemotoUsadoPorTodasAsFontes() {
        assertThat(ClassificacaoVaga.isRemoto("Remoto")).isTrue();
        assertThat(ClassificacaoVaga.isRemoto("Remoto (São Paulo, SP)")).isTrue();
        assertThat(ClassificacaoVaga.isRemoto("Híbrido (São Paulo, SP)")).isFalse();
        assertThat(ClassificacaoVaga.isRemoto("São Paulo, SP")).isFalse();
        assertThat(ClassificacaoVaga.isRemoto(null)).isFalse();
    }

    @Test
    void isInternacional_deveReconhecerVagaRemoteOkRestritaAOutroPais() {
        assertThat(ClassificacaoVaga.isInternacional("REMOTEOK", "Remoto (USA Only)")).isTrue();
        assertThat(ClassificacaoVaga.isInternacional("REMOTEOK", "Remoto (United Kingdom)")).isTrue();
        assertThat(ClassificacaoVaga.isInternacional("REMOTEOK", "Remoto (Europe)")).isTrue();
    }

    @Test
    void isInternacional_naoDeveMarcarVagaRemoteOkAbertaOuSemLocalizacao() {
        assertThat(ClassificacaoVaga.isInternacional("REMOTEOK", "Remoto (Worldwide)")).isFalse();
        assertThat(ClassificacaoVaga.isInternacional("REMOTEOK", "Remoto (Brazil)")).isFalse();
        assertThat(ClassificacaoVaga.isInternacional("REMOTEOK", "Remoto")).isFalse();
        assertThat(ClassificacaoVaga.isInternacional("REMOTEOK", null)).isFalse();
    }

    @Test
    void isInternacional_naoDeveDarFalsoPositivoPorSubstringSemBordaDePalavra() {
        // "uk" é uma das palavras curtas (<=3 letras) que exigem borda de palavra —
        // não pode casar dentro de "Duke Energy", por exemplo.
        assertThat(ClassificacaoVaga.isInternacional("REMOTEOK", "Remoto (Duke Energy Campus)")).isFalse();
    }

    @Test
    void isInternacional_deveIgnorarOutrasFontesMesmoComTextoParecido() {
        // Gupy e ProgramaThor são boards 100% nacionais — a checagem só vale pra RemoteOK.
        assertThat(ClassificacaoVaga.isInternacional("GUPY", "Remoto (São Paulo, São Paulo)")).isFalse();
        assertThat(ClassificacaoVaga.isInternacional("PROGRAMATHOR", "São Paulo, SP")).isFalse();
    }

    @Test
    void textoDeBusca_deveJuntarTituloEmpresaELocalSemAcentoEmMinusculas() {
        assertThat(ClassificacaoVaga.textoDeBusca("Estágio Java", "Açaí Tech", null))
                .isEqualTo("estagio java acai tech");
        assertThat(ClassificacaoVaga.textoDeBusca("Dev Jr", "ACME", "Remoto (Brasília)"))
                .isEqualTo("dev jr acme remoto (brasilia)");
    }
}
