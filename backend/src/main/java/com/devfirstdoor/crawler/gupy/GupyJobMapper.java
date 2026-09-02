package com.devfirstdoor.crawler.gupy;

import com.devfirstdoor.crawler.gupy.dto.GupyJobDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public class GupyJobMapper {

    private static final Logger log = LoggerFactory.getLogger(GupyJobMapper.class);

    public static final String FONTE = "GUPY";

    public Vaga paraVaga(GupyJobDto job, NivelVaga nivel) {
        return new Vaga(
                job.name().strip(),
                job.careerPageName() != null ? job.careerPageName().strip() : null,
                formatarLocal(job),
                nivel,
                job.jobUrl(),
                FONTE,
                parseDataPublicacao(job.publishedDate()),
                LocalDateTime.now()
        );
    }

    private String formatarLocal(GupyJobDto job) {
        boolean temCidade = job.city() != null && !job.city().isBlank();
        boolean temEstado = job.state() != null && !job.state().isBlank();
        boolean remota = Boolean.TRUE.equals(job.remotoOk());

        if (remota && temCidade && temEstado) {
            return "Remoto (" + job.city() + ", " + job.state() + ")";
        }
        if (remota && temCidade) {
            return "Remoto (" + job.city() + ")";
        }
        if (remota) {
            return "Remoto";
        }
        if (temCidade && temEstado) {
            return job.city() + ", " + job.state();
        }
        if (temCidade) {
            return job.city();
        }
        return "Não informado";
    }

    private LocalDate parseDataPublicacao(String publishedDate) {
        if (publishedDate == null || publishedDate.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(publishedDate).atZone(ZoneOffset.UTC).toLocalDate();
        } catch (DateTimeParseException e) {
            log.warn("Não foi possível interpretar a data de publicação '{}'", publishedDate);
            return null;
        }
    }
}
