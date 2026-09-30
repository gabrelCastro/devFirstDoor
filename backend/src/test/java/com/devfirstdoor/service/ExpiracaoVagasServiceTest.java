package com.devfirstdoor.service;

import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.domain.StatusVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.VagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static java.time.temporal.ChronoUnit.MINUTES;

@SpringBootTest
class ExpiracaoVagasServiceTest {

    @Autowired
    private VagaRepository vagaRepository;

    @Autowired
    private DeduplicacaoService deduplicacaoService;

    @Autowired
    private ExpiracaoVagasService expiracaoVagasService;

    @Autowired
    private HistoricoColetaService historicoColetaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final LocalDateTime agora = LocalDateTime.now();

    @BeforeEach
    void limparBanco() {
        vagaRepository.deleteAll();
    }

    @Test
    void removerExpiradas_deveMarcarComoExpiradasSoAsVagasDaFonteNaoVistasHaMaisDeNDias() {
        salvar("Estágio Java", "GUPY", agora.minusDays(8));
        salvar("Júnior Java", "GUPY", agora.minusDays(6));
        salvar("Java Intern", "REMOTEOK", agora.minusDays(30));

        int removidas = expiracaoVagasService.removerExpiradas("GUPY", agora);

        assertThat(removidas).isEqualTo(1);
        assertThat(vagaRepository.findAll()).hasSize(3);
        assertThat(statusDe("Estágio Java")).isEqualTo(StatusVaga.EXPIRADA);
        assertThat(statusDe("Júnior Java")).isEqualTo(StatusVaga.ATIVA);
        assertThat(statusDe("Java Intern")).isEqualTo(StatusVaga.ATIVA);
    }

    @Test
    void removerExpiradas_vagaSemDataUltimaVisitaDeveContarAPartirDaDataColeta() {
        salvar("Estágio Java", "GUPY", agora.minusDays(8));
        salvar("Júnior Java", "GUPY", agora.minusDays(1));
        jdbcTemplate.update("update vaga set data_ultima_visita = null");

        expiracaoVagasService.removerExpiradas("GUPY", agora);

        assertThat(statusDe("Estágio Java")).isEqualTo(StatusVaga.EXPIRADA);
        assertThat(statusDe("Júnior Java")).isEqualTo(StatusVaga.ATIVA);
    }

    @Test
    void coleta_deveAtualizarDataUltimaVisitaDaVagaQueReaparece_eRemoverAQueSumiu() {
        salvar("Estágio Java", "FALSO", agora.minusDays(10));
        salvar("Vaga Encerrada", "FALSO", agora.minusDays(10));
        Vaga antiga = vagaRepository.findAll().stream()
                .filter(v -> v.getTitulo().equals("Estágio Java"))
                .findFirst().orElseThrow();
        antiga.alterarStatus(StatusVaga.EXPIRADA);
        vagaRepository.save(antiga);

        Map<String, Integer> resultado = coletar(() -> List.of(
                vaga("Estágio Java", "FALSO", agora),
                vaga("Júnior Java", "FALSO", agora)));

        assertThat(resultado).containsEntry("FALSO", 1);
        List<Vaga> vagas = vagaRepository.findAll();
        assertThat(vagas).extracting(Vaga::getTitulo)
                .containsExactlyInAnyOrder("Estágio Java", "Vaga Encerrada", "Júnior Java");
        Vaga reapareceu = vagas.stream().filter(v -> v.getTitulo().equals("Estágio Java")).findFirst().orElseThrow();
        assertThat(reapareceu.getStatus()).isEqualTo(StatusVaga.ATIVA);
        assertThat(reapareceu.getDataColeta()).isCloseTo(agora.minusDays(10), within(1, MINUTES));
        assertThat(reapareceu.getDataUltimaVisita()).isCloseTo(LocalDateTime.now(), within(1, MINUTES));
        assertThat(statusDe("Vaga Encerrada")).isEqualTo(StatusVaga.EXPIRADA);
    }

    @Test
    void coleta_semVagasOuComFalhaNaoDeveExpirarNadaDaFonte() {
        salvar("Estágio Java", "FALSO", agora.minusDays(10));

        coletar(List::of);
        coletar(() -> {
            throw new IllegalStateException("fonte fora do ar");
        });

        assertThat(vagaRepository.findAll()).extracting(Vaga::getTitulo).containsExactly("Estágio Java");
    }

    private Map<String, Integer> coletar(Supplier<List<Vaga>> coleta) {
        VagaCrawler crawler = new VagaCrawler() {
            @Override
            public String getFonte() {
                return "FALSO";
            }

            @Override
            public List<Vaga> coletar() {
                return coleta.get();
            }
        };
        return new ColetaService(List.of(crawler), deduplicacaoService, vagaRepository, expiracaoVagasService,
                historicoColetaService, new AndamentoColeta()).executarTodos(OrigemColeta.MANUAL);
    }

    private void salvar(String titulo, String fonte, LocalDateTime dataColeta) {
        Vaga vaga = vaga(titulo, fonte, dataColeta);
        vaga.setHashDeduplicacao(deduplicacaoService.calcularHash(titulo, "Empresa", fonte));
        vagaRepository.save(vaga);
    }

    private Vaga vaga(String titulo, String fonte, LocalDateTime dataColeta) {
        return new Vaga(titulo, "Empresa", "Remoto", NivelVaga.ESTAGIO,
                "https://example.com/" + fonte + "/" + titulo.hashCode(), fonte, null, dataColeta);
    }

    private StatusVaga statusDe(String titulo) {
        return vagaRepository.findAll().stream()
                .filter(v -> v.getTitulo().equals(titulo))
                .map(Vaga::getStatus)
                .findFirst().orElseThrow();
    }
}
