package com.devfirstdoor.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SaudeCrawlerTest {

    @Test
    void desligada_deveSerDesligadaMesmoComHistoricoDeErro() {
        ExecucaoFonte erro = erro();

        assertThat(SaudeCrawler.calcular(false, Optional.of(erro), List.of())).isEqualTo(SaudeCrawler.DESLIGADA);
    }

    @Test
    void semExecucoes_deveSerSemDados() {
        assertThat(SaudeCrawler.calcular(true, Optional.empty(), List.of())).isEqualTo(SaudeCrawler.SEM_DADOS);
    }

    @Test
    void ultimaExecucaoComErro_deveSerFalha() {
        List<ExecucaoFonte> sucessos = List.of(sucesso(5), sucesso(5), sucesso(5));

        assertThat(SaudeCrawler.calcular(true, Optional.of(erro()), sucessos)).isEqualTo(SaudeCrawler.FALHA);
    }

    @Test
    void tresUltimosSucessosSemVagas_deveSerAlerta() {
        List<ExecucaoFonte> sucessos = List.of(sucesso(0), sucesso(0), sucesso(0));

        assertThat(SaudeCrawler.calcular(true, Optional.of(sucessos.get(0)), sucessos)).isEqualTo(SaudeCrawler.ALERTA);
    }

    @Test
    void algumDosTresUltimosSucessosComVagas_deveSerOk() {
        List<ExecucaoFonte> sucessos = List.of(sucesso(0), sucesso(0), sucesso(3));

        assertThat(SaudeCrawler.calcular(true, Optional.of(sucessos.get(0)), sucessos)).isEqualTo(SaudeCrawler.OK);
    }

    @Test
    void menosDeTresSucessosSemVagas_aindaNaoDeveSerAlerta() {
        List<ExecucaoFonte> sucessos = List.of(sucesso(0), sucesso(0));

        assertThat(SaudeCrawler.calcular(true, Optional.of(sucessos.get(0)), sucessos)).isEqualTo(SaudeCrawler.OK);
    }

    @Test
    void sucessoDepoisDeUmErro_deveVoltarAOk() {
        List<ExecucaoFonte> sucessos = List.of(sucesso(4));

        assertThat(SaudeCrawler.calcular(true, Optional.of(sucessos.get(0)), sucessos)).isEqualTo(SaudeCrawler.OK);
    }

    private static ExecucaoFonte sucesso(int encontradas) {
        return ExecucaoFonte.sucesso(null, "GUPY", LocalDateTime.now(), encontradas, 0, 0);
    }

    private static ExecucaoFonte erro() {
        return ExecucaoFonte.erro(null, "GUPY", LocalDateTime.now(), "HTML mudou");
    }
}
