package com.devfirstdoor.service;

import com.devfirstdoor.controller.dto.ExecucaoColetaResponse;
import com.devfirstdoor.domain.ExecucaoColeta;
import com.devfirstdoor.domain.ExecucaoFonte;
import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.domain.StatusExecucao;
import com.devfirstdoor.domain.StatusFonte;
import com.devfirstdoor.repository.ExecucaoColetaRepository;
import com.devfirstdoor.repository.ExecucaoFonteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HistoricoColetaServiceTest {

    @Autowired
    private HistoricoColetaService historicoColetaService;

    @Autowired
    private ExecucaoColetaRepository execucaoRepository;

    @Autowired
    private ExecucaoFonteRepository fonteRepository;

    @BeforeEach
    void limparBanco() {
        fonteRepository.deleteAll();
        execucaoRepository.deleteAll();
    }

    @Test
    void listar_deveDevolverAsMaisRecentesPrimeiroComOResultadoDeCadaFonte() {
        ExecucaoColeta antiga = historicoColetaService.iniciar(OrigemColeta.INICIAL);
        List<ExecucaoFonte> resultadosAntiga = List.of(
                ExecucaoFonte.sucesso(antiga, "GUPY", LocalDateTime.now(), 10, 3, 1));
        resultadosAntiga.forEach(historicoColetaService::registrar);
        historicoColetaService.finalizar(antiga, resultadosAntiga);

        ExecucaoColeta recente = historicoColetaService.iniciar(OrigemColeta.MANUAL);
        List<ExecucaoFonte> resultadosRecente = List.of(
                ExecucaoFonte.sucesso(recente, "GUPY", LocalDateTime.now(), 8, 0, 0),
                ExecucaoFonte.erro(recente, "REMOTEOK", LocalDateTime.now(), "fora do ar"));
        resultadosRecente.forEach(historicoColetaService::registrar);
        historicoColetaService.finalizar(recente, resultadosRecente);

        Page<ExecucaoColetaResponse> pagina = historicoColetaService.listar(PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent()).extracting(ExecucaoColetaResponse::origem)
                .containsExactly(OrigemColeta.MANUAL, OrigemColeta.INICIAL);

        ExecucaoColetaResponse primeira = pagina.getContent().get(0);
        assertThat(primeira.status()).isEqualTo(StatusExecucao.PARCIAL);
        assertThat(primeira.fim()).isNotNull();
        assertThat(primeira.fontes()).extracting(ExecucaoColetaResponse.Fonte::fonte).containsExactly("GUPY", "REMOTEOK");
        assertThat(primeira.fontes().get(1).status()).isEqualTo(StatusFonte.ERRO);
        assertThat(primeira.fontes().get(1).mensagemErro()).isEqualTo("fora do ar");

        ExecucaoColetaResponse.Fonte gupyAntiga = pagina.getContent().get(1).fontes().get(0);
        assertThat(gupyAntiga.encontradas()).isEqualTo(10);
        assertThat(gupyAntiga.novas()).isEqualTo(3);
        assertThat(gupyAntiga.expiradas()).isEqualTo(1);
    }

    @Test
    void iniciar_execucaoEmAndamentoDeveAparecerSemFimESemFontes() {
        historicoColetaService.iniciar(OrigemColeta.AGENDADA);

        ExecucaoColetaResponse execucao = historicoColetaService.listar(PageRequest.of(0, 10)).getContent().get(0);

        assertThat(execucao.status()).isEqualTo(StatusExecucao.EM_ANDAMENTO);
        assertThat(execucao.fim()).isNull();
        assertThat(execucao.fontes()).isEmpty();
    }

    @Test
    void listar_devePaginar() {
        for (int i = 0; i < 3; i++) {
            historicoColetaService.iniciar(OrigemColeta.AGENDADA);
        }

        Page<ExecucaoColetaResponse> segunda = historicoColetaService.listar(PageRequest.of(1, 2));

        assertThat(segunda.getTotalElements()).isEqualTo(3);
        assertThat(segunda.getContent()).hasSize(1);
    }
}
