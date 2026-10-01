package com.devfirstdoor.service;

import com.devfirstdoor.domain.ExecucaoColeta;
import com.devfirstdoor.domain.ExecucaoFonte;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.domain.SaudeCrawler;
import com.devfirstdoor.domain.StatusFonte;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.ExecucaoFonteRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificacaoServiceTest {

    private final ConfiguracaoService configuracaoService = mock(ConfiguracaoService.class);
    private final ExecucaoFonteRepository execucaoFonteRepository = mock(ExecucaoFonteRepository.class);
    private final List<Envio> envios = new ArrayList<>();
    private final ClienteTelegram cliente = (token, chatId, texto) -> envios.add(new Envio(token, chatId, texto));
    private final NotificacaoService service =
            new NotificacaoService(configuracaoService, execucaoFonteRepository, cliente);

    @Test
    void notificarVagasNovas_deveEnviarOsCamposEAgruparSemUltrapassarOLimite() {
        habilitar();
        List<Vaga> vagas = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            vagas.add(vaga("Desenvolvedor Java Júnior %02d %s".formatted(i, "x".repeat(230)), i));
        }

        service.notificarVagasNovas(vagas);

        assertThat(envios).hasSizeGreaterThan(1);
        assertThat(envios).allSatisfy(envio -> {
            assertThat(envio.token()).isEqualTo("token-secreto");
            assertThat(envio.chatId()).isEqualTo("123");
            assertThat(envio.texto()).hasSizeLessThanOrEqualTo(NotificacaoService.TAMANHO_MAXIMO_MENSAGEM);
            assertThat(envio.texto()).startsWith("Novas vagas no Dev First Door");
        });
        assertThat(envios).anySatisfy(envio -> assertThat(envio.texto())
                .contains("Empresa: Empresa", "Nível: JUNIOR", "Local: Remoto", "https://exemplo.com/vaga/0"));
    }

    @Test
    void notificarMudancaSaude_deveAvisarAlertaUmaVezEQuandoVoltarAOk() {
        habilitar();
        ExecucaoFonte vazio1 = sucesso(0);
        ExecucaoFonte vazio2 = sucesso(0);
        when(execucaoFonteRepository.findFirstByFonteOrderByInicioDescIdDesc("GUPY"))
                .thenReturn(Optional.of(vazio2));
        when(execucaoFonteRepository.findTop3ByFonteAndStatusOrderByInicioDescIdDesc("GUPY", StatusFonte.SUCESSO))
                .thenReturn(List.of(vazio2, vazio1));
        SaudeCrawler anterior = service.consultarSaude("GUPY", true);

        ExecucaoFonte vazio3 = sucesso(0);
        when(execucaoFonteRepository.findFirstByFonteOrderByInicioDescIdDesc("GUPY"))
                .thenReturn(Optional.of(vazio3));
        when(execucaoFonteRepository.findTop3ByFonteAndStatusOrderByInicioDescIdDesc("GUPY", StatusFonte.SUCESSO))
                .thenReturn(List.of(vazio3, vazio2, vazio1));

        service.notificarMudancaSaude("GUPY", true, anterior);
        service.notificarMudancaSaude("GUPY", true, SaudeCrawler.ALERTA);

        ExecucaoFonte normal = sucesso(4);
        when(execucaoFonteRepository.findFirstByFonteOrderByInicioDescIdDesc("GUPY"))
                .thenReturn(Optional.of(normal));
        when(execucaoFonteRepository.findTop3ByFonteAndStatusOrderByInicioDescIdDesc("GUPY", StatusFonte.SUCESSO))
                .thenReturn(List.of(normal, vazio3, vazio2));
        service.notificarMudancaSaude("GUPY", true, SaudeCrawler.ALERTA);

        assertThat(envios).extracting(Envio::texto).containsExactly(
                "Saúde do crawler GUPY mudou de OK para ALERTA.",
                "Saúde do crawler GUPY mudou de ALERTA para OK.");
    }

    @Test
    void notificarMudancaSaude_deveAvisarPrimeiraFalhaSemRepetir() {
        habilitar();
        ExecucaoColeta execucao = new ExecucaoColeta(OrigemColeta.AGENDADA, LocalDateTime.now());
        ExecucaoFonte erro = ExecucaoFonte.erro(execucao, "LEVER", LocalDateTime.now(), "fora do ar");
        when(execucaoFonteRepository.findFirstByFonteOrderByInicioDescIdDesc("LEVER"))
                .thenReturn(Optional.of(erro));
        when(execucaoFonteRepository.findTop3ByFonteAndStatusOrderByInicioDescIdDesc("LEVER", StatusFonte.SUCESSO))
                .thenReturn(List.of());

        service.notificarMudancaSaude("LEVER", true, SaudeCrawler.SEM_DADOS);
        service.notificarMudancaSaude("LEVER", true, SaudeCrawler.FALHA);

        assertThat(envios).extracting(Envio::texto)
                .containsExactly("Saúde do crawler LEVER mudou de SEM_DADOS para FALHA.");
    }

    @Test
    void enviarTeste_deveExigirCredenciaisMesmoComNotificacoesDesligadas() {
        when(configuracaoService.obterTelegram())
                .thenReturn(new ConfiguracaoService.ConfiguracaoTelegram(false, "", ""));

        assertThatThrownBy(service::enviarTeste)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("token e o chat id");
        assertThat(envios).isEmpty();
    }

    private void habilitar() {
        when(configuracaoService.obterTelegram())
                .thenReturn(new ConfiguracaoService.ConfiguracaoTelegram(true, "token-secreto", "123"));
    }

    private static ExecucaoFonte sucesso(int encontradas) {
        ExecucaoColeta execucao = new ExecucaoColeta(OrigemColeta.AGENDADA, LocalDateTime.now());
        return ExecucaoFonte.sucesso(execucao, "GUPY", LocalDateTime.now(), encontradas, 0, 0);
    }

    private static Vaga vaga(String titulo, int indice) {
        return new Vaga(titulo, "Empresa", "Remoto", NivelVaga.JUNIOR,
                "https://exemplo.com/vaga/" + indice, "GUPY", null, LocalDateTime.now());
    }

    private record Envio(String token, String chatId, String texto) {
    }
}
