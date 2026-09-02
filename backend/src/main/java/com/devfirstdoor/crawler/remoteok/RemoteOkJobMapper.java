package com.devfirstdoor.crawler.remoteok;

import com.devfirstdoor.crawler.remoteok.dto.RemoteOkJobDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public class RemoteOkJobMapper {

    private static final Logger log = LoggerFactory.getLogger(RemoteOkJobMapper.class);

    public static final String FONTE = "REMOTEOK";

    public Vaga paraVaga(RemoteOkJobDto job, NivelVaga nivel) {
        return new Vaga(
                job.position().strip(),
                job.company() != null ? job.company().strip() : null,
                formatarLocal(job),
                nivel,
                job.url(),
                FONTE,
                parseDataPublicacao(job.date()),
                LocalDateTime.now()
        );
    }

    private String formatarLocal(RemoteOkJobDto job) {
        String cidade = job.location() == null ? "" : job.location().replaceAll(",\\s*$", "").trim();
        return cidade.isEmpty() ? "Remoto" : "Remoto (" + cidade + ")";
    }

    private LocalDate parseDataPublicacao(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(date).toLocalDate();
        } catch (DateTimeParseException e) {
            log.warn("Não foi possível interpretar a data de publicação '{}'", date);
            return null;
        }
    }
}
