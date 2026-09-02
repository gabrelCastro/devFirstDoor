package com.devfirstdoor.crawler.programathor;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;

import java.time.LocalDateTime;

public class ProgramathorJobMapper {

    public static final String FONTE = "PROGRAMATHOR";

    public Vaga paraVaga(ProgramathorJobDto job, NivelVaga nivel) {
        return new Vaga(
                job.titulo(),
                job.empresa(),
                job.local() != null && !job.local().isBlank() ? job.local() : "Não informado",
                nivel,
                job.link(),
                FONTE,
                null, // a listagem da ProgramaThor não expõe data de publicação
                LocalDateTime.now()
        );
    }
}
