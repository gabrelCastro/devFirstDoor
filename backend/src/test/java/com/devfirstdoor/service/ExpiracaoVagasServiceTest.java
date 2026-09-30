package com.devfirstdoor.service;

import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.OrigemColeta;
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
    void removerExpiradas_deveRemoverSoAsVagasDaFonteNaoVistasHaMaisDeNDias() {
        salvar("Estágio Java", "GUPY", agora.minusDays(8));
        salvar("Júnior Java", "GUPY", agora.minusDays(6));
        salvar("Java Intern", "REMOTEOK", agora.minusDays(30));

        int removidas = expiracaoVagasService.removerExpiradas("GUPY", agora);

        assertThat(removidas).isEqualTo(1);
        assertThat(vagaRepository.findAll()).extracting(Vaga::getTitulo)
                .containsExactlyInAnyOrder("Júnior Java", "Java Intern");
    }

    @Test
    void removerExpiradas_vagaSemDataUltimaVisitaDeveContarAPartirDaDataColeta() {
        salvar("Estágio Java", "GUPY", agora.minusDays(8));
        salvar("Júnior Java", "GUPY", agora.minusDays(1));
        jdbcTemplate.update("update vaga set data_ultima_visita = null");

        expiracaoVagasService.removerExpiradas("GUPY", agora);

        assertThat(vagaRepository.findAll()).extracting(Vaga::getTitulo).containsExactly("Júnior Java");
    }

    @Test
    void coleta_deveAtualizarDataUltimaVisitaDaVagaQueReaparece_eRemoverAQueSumiu() {
        salvar("Estágio Java", "FALSO", agora.minusDays(10));
        salvar("Vaga Encerrada", "FALSO", agora.minusDays(10));

        Map<String, Integer> resultado = coletar(() -> List.of(
                vaga("Estágio Java", "FALSO", agora),
                vaga("Júnior Java", "FALSO", agora)));

        assertThat(resultado).containsEntry("FALSO", 1);
        List<Vaga> vagas = vagaRepository.findAll();
        assertThat(vagas).extracting(Vaga::getTitulo).containsExactlyInAnyOrder("Estágio Java", "Júnior Java");
        Vaga reapareceu = vagas.stream().filter(v -> v.getTitulo().equals("Estágio Java")).findFirst().orElseThrow();
        assertThat(reapareceu.getDataColeta()).isCloseTo(agora.minusDays(10), within(1, MINUTES));
        assertThat(reapareceu.getDataUltimaVisita()).isCloseTo(LocalDateTime.now(), within(1, MINUTES));
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
                historicoColetaService).executarTodos(OrigemColeta.MANUAL);
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
}
