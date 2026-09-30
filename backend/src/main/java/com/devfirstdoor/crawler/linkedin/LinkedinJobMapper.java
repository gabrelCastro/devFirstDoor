package com.devfirstdoor.crawler.linkedin;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;

import java.time.LocalDateTime;

public class LinkedinJobMapper {

    public static final String FONTE = "LINKEDIN";

    /**
     * Segue a convenção das outras fontes de marcar a modalidade no próprio local
     * ("Remoto (São Paulo, SP)"), que é o que a API usa para montar a seção Remoto.
     */
    public Vaga paraVaga(LinkedinJobDto job, NivelVaga nivel, LinkedinModalidade modalidade) {
        return new Vaga(
                job.titulo(),
                job.empresa(),
                formatarLocal(job.local(), modalidade),
                nivel,
                job.link(),
                FONTE,
                job.dataPublicacao(),
                LocalDateTime.now()
        );
    }

    private String formatarLocal(String local, LinkedinModalidade modalidade) {
        boolean temLocal = local != null && !local.isBlank();
        String prefixo = switch (modalidade) {
            case REMOTO -> "Remoto";
            case HIBRIDO -> "Híbrido";
            case PRESENCIAL, DESCONHECIDA -> null;
        };
        if (prefixo == null) {
            return temLocal ? local : "Não informado";
        }
        return temLocal ? prefixo + " (" + local + ")" : prefixo;
    }
}
