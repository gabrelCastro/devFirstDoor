package com.devfirstdoor.service;

import com.devfirstdoor.controller.dto.ConfiguracaoAdminRequest;
import com.devfirstdoor.repository.ConfiguracaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ConfiguracaoServiceTest {

    @Autowired
    private ConfiguracaoService service;

    @Autowired
    private ConfiguracaoRepository repository;

    @BeforeEach
    void limparConfiguracao() {
        repository.deleteAll();
    }

    @Test
    void obter_semValorPersistido_deveUsarOsPadroesDoYml() {
        ConfiguracaoColeta configuracao = service.obter();

        assertThat(configuracao.fonteLigada("GUPY")).isTrue();
        assertThat(configuracao.fonteLigada("LINKEDIN")).isFalse();
        assertThat(configuracao.termosBuscaGupy()).contains("java");
        assertThat(configuracao.intervaloColeta().toHours()).isEqualTo(6);
        assertThat(configuracao.diasParaExpirar()).isEqualTo(7);
        assertThat(configuracao.pausaLinkedinMs()).isEqualTo(3000);
    }

    @Test
    void salvar_devePersistirEEntregarAAlteracaoNaProximaLeitura() {
        service.salvar(requestPadrao(
                List.of("primeiro termo"), List.of("empresa-a"), 45L, 12, true));

        ConfiguracaoColeta seguinte = service.obter();

        assertThat(repository.count()).isEqualTo(10);
        assertThat(seguinte.termosBuscaGupy()).containsExactly("primeiro termo");
        assertThat(seguinte.empresasGreenhouse()).containsExactly("empresa-a");
        assertThat(seguinte.intervaloColeta().toMinutes()).isEqualTo(45);
        assertThat(seguinte.diasParaExpirar()).isEqualTo(12);
        assertThat(seguinte.agendamentoPausado()).isTrue();
    }

    @Test
    void salvar_linkedinLigadoSemChaveMestra_deveRecusar() {
        ConfiguracaoAdminRequest request = requestPadrao(
                List.of("java"), List.of(), 30L, 1, false);
        request.fontesLigadas().put("LINKEDIN", true);

        assertThatThrownBy(() -> service.salvar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LINKEDIN_ENABLED=true");
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void salvar_deveValidarLimitesEItensVazios() {
        assertThatThrownBy(() -> service.salvar(requestPadrao(
                List.of("java", " "), List.of(), 30L, 1, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itens vazios");
        assertThatThrownBy(() -> service.salvar(requestPadrao(
                List.of("java"), List.of(), 29L, 1, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("30 minutos");
        assertThatThrownBy(() -> service.salvar(requestPadrao(
                List.of("java"), List.of(), 30L, 0, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pelo menos 1");
    }

    private static ConfiguracaoAdminRequest requestPadrao(List<String> termosGupy,
                                                           List<String> empresasGreenhouse,
                                                           long intervalo, int dias, boolean pausado) {
        Map<String, Boolean> fontes = new LinkedHashMap<>();
        ConfiguracaoService.FONTES.forEach(fonte -> fontes.put(fonte, !"LINKEDIN".equals(fonte)));
        return new ConfiguracaoAdminRequest(
                fontes,
                termosGupy,
                List.of("estágio java"),
                empresasGreenhouse,
                List.of(),
                intervalo,
                dias,
                1000L,
                500L,
                pausado
        );
    }
}
