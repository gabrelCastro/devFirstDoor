package com.devfirstdoor.crawler.remoteok;

import com.devfirstdoor.crawler.remoteok.dto.RemoteOkJobDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.util.TextNormalizer;

import java.util.Optional;

public class RemoteOkVagaClassifier {

    private final RemoteOkCrawlerProperties properties;

    public RemoteOkVagaClassifier(RemoteOkCrawlerProperties properties) {
        this.properties = properties;
    }

    public Optional<NivelVaga> classificarNivel(RemoteOkJobDto job) {
        String titulo = job.position();
        if (TextNormalizer.contemAlgumaPalavra(titulo, properties.getPalavrasEstagio())) {
            return Optional.of(NivelVaga.ESTAGIO);
        }
        if (TextNormalizer.contemAlgumaPalavra(titulo, properties.getPalavrasJunior())) {
            return Optional.of(NivelVaga.JUNIOR);
        }
        return Optional.empty();
    }

    public boolean isRelevanteParaTech(RemoteOkJobDto job) {
        return TextNormalizer.contemAlgumaPalavra(job.position(), properties.getPalavrasTech());
    }
}
