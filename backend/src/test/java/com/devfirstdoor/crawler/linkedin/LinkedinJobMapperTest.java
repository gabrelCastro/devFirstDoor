package com.devfirstdoor.crawler.linkedin;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LinkedinJobMapperTest {

    private final LinkedinJobMapper mapper = new LinkedinJobMapper();

    @Test
    void paraVaga_devePrefixarOLocalComAModalidade() {
        assertThat(local("São Paulo, SP", LinkedinModalidade.REMOTO)).isEqualTo("Remoto (São Paulo, SP)");
        assertThat(local("São Paulo, SP", LinkedinModalidade.HIBRIDO)).isEqualTo("Híbrido (São Paulo, SP)");
        assertThat(local("São Paulo, SP", LinkedinModalidade.PRESENCIAL)).isEqualTo("São Paulo, SP");
        assertThat(local("São Paulo, SP", LinkedinModalidade.DESCONHECIDA)).isEqualTo("São Paulo, SP");
    }

    @Test
    void paraVaga_semLocal() {
        assertThat(local(null, LinkedinModalidade.REMOTO)).isEqualTo("Remoto");
        assertThat(local(" ", LinkedinModalidade.PRESENCIAL)).isEqualTo("Não informado");
    }

    @Test
    void paraVaga_deveCopiarOsDemaisCampos() {
        LinkedinJobDto job = new LinkedinJobDto("42", "Dev Júnior", "ACME", "Recife, PE",
                "https://www.linkedin.com/jobs/view/42", LocalDate.of(2026, 9, 10));

        Vaga vaga = mapper.paraVaga(job, NivelVaga.JUNIOR, LinkedinModalidade.REMOTO);

        assertThat(vaga.getTitulo()).isEqualTo("Dev Júnior");
        assertThat(vaga.getEmpresa()).isEqualTo("ACME");
        assertThat(vaga.getNivel()).isEqualTo(NivelVaga.JUNIOR);
        assertThat(vaga.getLink()).isEqualTo("https://www.linkedin.com/jobs/view/42");
        assertThat(vaga.getFonte()).isEqualTo("LINKEDIN");
        assertThat(vaga.getDataPublicacao()).isEqualTo(LocalDate.of(2026, 9, 10));
    }

    private String local(String local, LinkedinModalidade modalidade) {
        LinkedinJobDto job = new LinkedinJobDto("1", "Dev Júnior", "ACME", local, "https://www.linkedin.com/jobs/view/1", null);
        return mapper.paraVaga(job, NivelVaga.JUNIOR, modalidade).getLocal();
    }
}
