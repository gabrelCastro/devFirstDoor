package com.devfirstdoor.crawler.greenhouse;

import com.devfirstdoor.crawler.greenhouse.dto.GreenhouseJobDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public class GreenhouseJobMapper {

    private static final Logger log = LoggerFactory.getLogger(GreenhouseJobMapper.class);

    public static final String FONTE = "GREENHOUSE";

    /**
     * @param empresa token do board, usado como nome quando a API não traz "company_name"
     */
    public Vaga paraVaga(GreenhouseJobDto job, String empresa, NivelVaga nivel) {
        return new Vaga(
                job.title().strip(),
                job.companyName() != null && !job.companyName().isBlank() ? job.companyName().strip() : empresa,
                formatarLocal(job.nomeDoLocal()),
                nivel,
                job.absoluteUrl(),
                FONTE,
                parseDataPublicacao(job.firstPublished() != null ? job.firstPublished() : job.updatedAt()),
                LocalDateTime.now()
        );
    }

    /**
     * Só coletamos vagas remotas, então o local sempre começa com "Remoto" (ver
     * ClassificacaoVaga#isRemoto); o texto da API fica entre parênteses para a
     * classificação de vaga restrita a outro país.
     */
    private String formatarLocal(String local) {
        String texto = local == null ? "" : local.strip();
        return texto.isEmpty() || texto.equalsIgnoreCase("remote") ? "Remoto" : "Remoto (" + texto + ")";
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
