package com.devfirstdoor.service;

import com.devfirstdoor.controller.dto.ResumoCandidaturasResponse;
import com.devfirstdoor.domain.Candidatura;
import com.devfirstdoor.domain.EtapaCandidatura;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidaturaServiceTest {

    /** Quarta-feira, 2026-10-07 10:00. */
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 7, 10, 0);

    @Test
    void resumo_deveCalcularTaxaTempoEFollowUps() {
        // Enviada há 20 dias, respondida (entrevista) 6 dias depois do envio.
        Candidatura respondida = candidatura(1, EtapaCandidatura.ENTREVISTA, AGORA.minusDays(14), AGORA.minusDays(20));
        // Enviada há 16 dias, parada em "candidatei" desde então: precisa de follow-up.
        Candidatura parada = candidatura(2, EtapaCandidatura.CANDIDATADO, AGORA.minusDays(16), AGORA.minusDays(16));
        // Enviada hoje, recente: sem follow-up.
        Candidatura recente = candidatura(3, EtapaCandidatura.CANDIDATADO, AGORA, AGORA);
        // Reprovada (conta como resposta), 2 dias até responder.
        Candidatura reprovada = candidatura(4, EtapaCandidatura.REPROVADO, AGORA.minusDays(3), AGORA.minusDays(5));
        // Só interesse, com próximo passo vencido: follow-up, mas não conta como enviada.
        Candidatura interesse = candidatura(5, EtapaCandidatura.INTERESSE, AGORA.minusDays(1), null);
        interesse.definirProximoPasso("Pedir indicação", AGORA.toLocalDate().minusDays(1));

        ResumoCandidaturasResponse resumo = CandidaturaService.calcularResumo(
                List.of(respondida, parada, recente, reprovada, interesse),
                Map.of(1L, AGORA.minusDays(14), 4L, AGORA.minusDays(3)),
                AGORA);

        assertThat(resumo.total()).isEqualTo(5);
        assertThat(resumo.ativas()).isEqualTo(4);
        assertThat(resumo.encerradas()).isEqualTo(1);
        assertThat(resumo.enviadas()).isEqualTo(4);
        assertThat(resumo.responderam()).isEqualTo(2);
        assertThat(resumo.taxaResposta()).isEqualTo(0.5);
        assertThat(resumo.diasMedioAteResposta()).isEqualTo(4.0);
        assertThat(resumo.followUps()).isEqualTo(2);
        assertThat(resumo.porEtapa()).containsEntry(EtapaCandidatura.CANDIDATADO, 2).containsEntry(EtapaCandidatura.OFERTA, 0);
    }

    @Test
    void resumo_deveAgruparEnviosPorSemanaComecandoNaSegunda() {
        Candidatura estaSemana = candidatura(1, EtapaCandidatura.CANDIDATADO, AGORA, AGORA.minusDays(2)); // segunda
        Candidatura semanaPassada = candidatura(2, EtapaCandidatura.CANDIDATADO, AGORA, AGORA.minusDays(3)); // domingo
        Candidatura antiga = candidatura(3, EtapaCandidatura.CANDIDATADO, AGORA, AGORA.minusWeeks(20));

        ResumoCandidaturasResponse resumo = CandidaturaService.calcularResumo(
                List.of(estaSemana, semanaPassada, antiga), Map.of(), AGORA);

        assertThat(resumo.semanas()).hasSize(12);
        assertThat(resumo.semanas().get(11).inicio()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(resumo.semanas().get(11).enviadas()).isEqualTo(1);
        assertThat(resumo.semanas().get(10).enviadas()).isEqualTo(1);
        assertThat(resumo.semanas().stream().mapToInt(ResumoCandidaturasResponse.Semana::enviadas).sum()).isEqualTo(2);
        assertThat(resumo.enviadasNoMes()).isEqualTo(2); // dias 4 e 5 de outubro
    }

    @Test
    void resumo_semEnvios_deveDeixarAsTaxasNulas() {
        ResumoCandidaturasResponse resumo = CandidaturaService.calcularResumo(List.of(), Map.of(), AGORA);
        assertThat(resumo.taxaResposta()).isNull();
        assertThat(resumo.diasMedioAteResposta()).isNull();
        assertThat(resumo.semanas()).allSatisfy(semana -> assertThat(semana.enviadas()).isZero());
    }

    @Test
    void link_deveAceitarSoHttpComHost() {
        assertThat(CandidaturaService.link(" https://empresa.com/vaga ")).isEqualTo("https://empresa.com/vaga");
        assertThat(CandidaturaService.link("")).isNull();
        for (String invalido : List.of("javascript:alert(1)", "JAVASCRIPT:alert(1)", "data:text/html,x",
                "empresa.com/vaga", "http://", "ftp://empresa.com")) {
            assertThatThrownBy(() -> CandidaturaService.link(invalido)).isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test
    void csv_deveNeutralizarFormulas() {
        assertThat(CandidaturasCsv.campo("=SOMA(A1)")).isEqualTo("\"'=SOMA(A1)\"");
        assertThat(CandidaturasCsv.campo("-1")).isEqualTo("\"'-1\"");
        assertThat(CandidaturasCsv.campo("Banco \"X\"")).isEqualTo("\"Banco \"\"X\"\"\"");
        assertThat(CandidaturasCsv.campo(null)).isEmpty();
    }

    private static Candidatura candidatura(long id, EtapaCandidatura etapa, LocalDateTime etapaDesde, LocalDateTime envio) {
        Candidatura c = Candidatura.externa(null, "Dev", "Empresa", null, null, etapa, etapaDesde);
        c.definirDataCandidatura(envio == null ? null : envio.toLocalDate());
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
