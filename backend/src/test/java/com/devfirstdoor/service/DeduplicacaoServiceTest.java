package com.devfirstdoor.service;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.VagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeduplicacaoServiceTest {

    @Mock
    private VagaRepository vagaRepository;

    private DeduplicacaoService deduplicacaoService;

    @BeforeEach
    void setUp() {
        deduplicacaoService = new DeduplicacaoService(vagaRepository);
    }

    @Test
    void calcularHash_deveSerIgualParaVariacoesDeMaiusculasAcentosEEspacos() {
        String hash1 = deduplicacaoService.calcularHash("Desenvolvedor Júnior", "Empresa XPTO", "GUPY");
        String hash2 = deduplicacaoService.calcularHash("  desenvolvedor   junior  ", "empresa xpto", "gupy");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void calcularHash_deveSerDiferenteParaVagasDistintas() {
        String hash1 = deduplicacaoService.calcularHash("Desenvolvedor Júnior", "Empresa A", "GUPY");
        String hash2 = deduplicacaoService.calcularHash("Desenvolvedor Júnior", "Empresa B", "GUPY");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void isDuplicada_deveConsultarORepositorioPeloHash() {
        when(vagaRepository.existsByHashDeduplicacao("abc123")).thenReturn(true);

        assertThat(deduplicacaoService.isDuplicada("abc123")).isTrue();
    }

    @Test
    void filtrarNovas_deveDescartarVagaQueJaExisteNoBanco() {
        Vaga existente = criarVaga("Estágio em TI", "Empresa Já Cadastrada", "https://gupy.io/job/1");
        when(vagaRepository.existsByHashDeduplicacao(anyString())).thenReturn(true);

        List<Vaga> novas = deduplicacaoService.filtrarNovas(List.of(existente));

        assertThat(novas).isEmpty();
    }

    @Test
    void filtrarNovas_deveManterVagaInedita_eAtribuirOHash() {
        Vaga inedita = criarVaga("Estágio em TI", "Empresa Nova", "https://gupy.io/job/2");
        when(vagaRepository.existsByHashDeduplicacao(anyString())).thenReturn(false);

        List<Vaga> novas = deduplicacaoService.filtrarNovas(List.of(inedita));

        assertThat(novas).hasSize(1);
        assertThat(novas.get(0).getHashDeduplicacao()).isNotBlank();
    }

    @Test
    void filtrarNovas_deveRemoverDuplicataDentroDoMesmoLote() {
        Vaga primeira = criarVaga("Desenvolvedor Júnior", "Empresa X", "https://gupy.io/job/3");
        Vaga duplicataComOutroLink = criarVaga("desenvolvedor   junior", "empresa x", "https://gupy.io/job/4");
        when(vagaRepository.existsByHashDeduplicacao(anyString())).thenReturn(false);

        List<Vaga> novas = deduplicacaoService.filtrarNovas(List.of(primeira, duplicataComOutroLink));

        assertThat(novas).hasSize(1);
    }

    private Vaga criarVaga(String titulo, String empresa, String link) {
        return new Vaga(titulo, empresa, "São Paulo, SP", NivelVaga.ESTAGIO, link, "GUPY",
                LocalDate.now(), LocalDateTime.now());
    }
}
