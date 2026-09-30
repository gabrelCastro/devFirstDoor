package com.devfirstdoor.crawler.lever;

import com.devfirstdoor.crawler.lever.dto.LeverPostingDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class LeverJobMapper {

    public static final String FONTE = "LEVER";

    /**
     * @param empresa identificador da empresa no Lever, usado como nome porque a
     *                Postings API não traz o nome da empresa
     */
    public Vaga paraVaga(LeverPostingDto posting, String empresa, NivelVaga nivel) {
        return new Vaga(
                posting.text().strip(),
                empresa,
                formatarLocal(posting.local()),
                nivel,
                posting.hostedUrl(),
                FONTE,
                posting.createdAt() != null ? LocalDate.ofInstant(Instant.ofEpochMilli(posting.createdAt()), ZoneOffset.UTC) : null,
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
}
