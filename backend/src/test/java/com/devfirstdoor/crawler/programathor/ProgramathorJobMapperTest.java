package com.devfirstdoor.crawler.programathor;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProgramathorJobMapperTest {

    private final ProgramathorJobMapper mapper = new ProgramathorJobMapper();

    @Test
    void paraVaga_deveMapearCamposBasicos() {
        ProgramathorJobDto job = new ProgramathorJobDto(
                "Estágio de Produto 100% Remoto", "Empresa XPTO", "Remoto",
                "https://programathor.com.br/jobs/33528-estagio-remoto");

        Vaga vaga = mapper.paraVaga(job, NivelVaga.ESTAGIO);

        assertThat(vaga.getTitulo()).isEqualTo("Estágio de Produto 100% Remoto");
        assertThat(vaga.getEmpresa()).isEqualTo("Empresa XPTO");
        assertThat(vaga.getLocal()).isEqualTo("Remoto");
        assertThat(vaga.getNivel()).isEqualTo(NivelVaga.ESTAGIO);
        assertThat(vaga.getLink()).isEqualTo("https://programathor.com.br/jobs/33528-estagio-remoto");
        assertThat(vaga.getFonte()).isEqualTo("PROGRAMATHOR");
        assertThat(vaga.getDataPublicacao()).isNull();
    }

    @Test
    void paraVaga_deveUsarNaoInformadoQuandoLocalAusente() {
        ProgramathorJobDto job = new ProgramathorJobDto("Desenvolvedor Júnior", "Empresa Y", null,
                "https://programathor.com.br/jobs/1");

        Vaga vaga = mapper.paraVaga(job, NivelVaga.JUNIOR);

        assertThat(vaga.getLocal()).isEqualTo("Não informado");
    }
}
