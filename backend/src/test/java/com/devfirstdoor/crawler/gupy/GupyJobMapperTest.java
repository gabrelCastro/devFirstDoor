package com.devfirstdoor.crawler.gupy;

import com.devfirstdoor.crawler.gupy.dto.GupyJobDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GupyJobMapperTest {

    private final GupyJobMapper mapper = new GupyJobMapper();

    @Test
    void paraVaga_deveMapearCamposBasicosDoJob() {
        GupyJobDto job = new GupyJobDto(10L, "Desenvolvedor Júnior", "Empresa Teste",
                "Belo Horizonte", "Minas Gerais", false, "vacancy_type_effective",
                "2026-08-15T10:30:00.000Z", "https://empresa.gupy.io/job/10");

        Vaga vaga = mapper.paraVaga(job, NivelVaga.JUNIOR);

        assertThat(vaga.getTitulo()).isEqualTo("Desenvolvedor Júnior");
        assertThat(vaga.getEmpresa()).isEqualTo("Empresa Teste");
        assertThat(vaga.getLocal()).isEqualTo("Belo Horizonte, Minas Gerais");
        assertThat(vaga.getNivel()).isEqualTo(NivelVaga.JUNIOR);
        assertThat(vaga.getLink()).isEqualTo("https://empresa.gupy.io/job/10");
        assertThat(vaga.getFonte()).isEqualTo("GUPY");
        assertThat(vaga.getDataPublicacao()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void paraVaga_deveUsarRemotoQuandoNaoHaCidadeMasIsRemoteWorkForVerdadeiro() {
        GupyJobDto job = new GupyJobDto(11L, "Estágio TI", "Empresa Remota",
                null, null, true, "vacancy_type_internship",
                "2026-08-20T09:00:00.000Z", "https://empresa.gupy.io/job/11");

        Vaga vaga = mapper.paraVaga(job, NivelVaga.ESTAGIO);

        assertThat(vaga.getLocal()).isEqualTo("Remoto");
    }

    @Test
    void paraVaga_devePriorizarRemotoMostrandoACidadeEntreParenteses() {
        GupyJobDto job = new GupyJobDto(15L, "Desenvolvedor Júnior", "Empresa Híbrida",
                "São Paulo", "São Paulo", true, "vacancy_type_effective",
                "2026-08-20T09:00:00.000Z", "https://empresa.gupy.io/job/15");

        Vaga vaga = mapper.paraVaga(job, NivelVaga.JUNIOR);

        assertThat(vaga.getLocal()).isEqualTo("Remoto (São Paulo, São Paulo)");
    }

    @Test
    void paraVaga_deveUsarApenasCidadeQuandoEstadoAusente() {
        GupyJobDto job = new GupyJobDto(12L, "Estágio TI", "Empresa X",
                "Curitiba", null, false, "vacancy_type_internship",
                "2026-08-20T09:00:00.000Z", "https://empresa.gupy.io/job/12");

        Vaga vaga = mapper.paraVaga(job, NivelVaga.ESTAGIO);

        assertThat(vaga.getLocal()).isEqualTo("Curitiba");
    }

    @Test
    void paraVaga_deveMarcarComoNaoInformadoQuandoNaoHaLocalNemRemoto() {
        GupyJobDto job = new GupyJobDto(13L, "Estágio TI", "Empresa Y",
                null, null, false, "vacancy_type_internship",
                "2026-08-20T09:00:00.000Z", "https://empresa.gupy.io/job/13");

        Vaga vaga = mapper.paraVaga(job, NivelVaga.ESTAGIO);

        assertThat(vaga.getLocal()).isEqualTo("Não informado");
    }

    @Test
    void paraVaga_deveTolerarDataDePublicacaoInvalidaOuAusente() {
        GupyJobDto job = new GupyJobDto(14L, "Estágio TI", "Empresa Z",
                "Recife", "PE", false, "vacancy_type_internship",
                "data-invalida", "https://empresa.gupy.io/job/14");

        Vaga vaga = mapper.paraVaga(job, NivelVaga.ESTAGIO);

        assertThat(vaga.getDataPublicacao()).isNull();
    }
}
