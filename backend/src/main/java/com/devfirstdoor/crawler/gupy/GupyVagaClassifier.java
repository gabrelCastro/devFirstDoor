package com.devfirstdoor.crawler.gupy;

import com.devfirstdoor.crawler.gupy.dto.GupyJobDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.util.TextNormalizer;

import java.util.Optional;

/**
 * A API da Gupy só expõe filtro nativo por tipo de contrato (estágio, efetivo,
 * aprendiz, ...), sem noção de senioridade ou área. Por isso a classificação de
 * nível e a relevância para tecnologia são feitas aqui, por palavra-chave no título.
 */
public class GupyVagaClassifier {

    private static final String TIPO_ESTAGIO = "vacancy_type_internship";
    private static final String TIPO_EFETIVO = "vacancy_type_effective";

    private final GupyCrawlerProperties properties;

    public GupyVagaClassifier(GupyCrawlerProperties properties) {
        this.properties = properties;
    }

    public Optional<NivelVaga> classificarNivel(GupyJobDto job) {
        if (job.type() == null) {
            return Optional.empty();
        }
        if (TIPO_ESTAGIO.equals(job.type())) {
            return Optional.of(NivelVaga.ESTAGIO);
        }
        if (TIPO_EFETIVO.equals(job.type()) && contemPalavraJunior(job.name())) {
            return Optional.of(NivelVaga.JUNIOR);
        }
        return Optional.empty();
    }

    public boolean isRelevanteParaTech(GupyJobDto job) {
        return TextNormalizer.contemAlgumaPalavra(job.name(), properties.getPalavrasTech());
    }

    public boolean isRemota(GupyJobDto job) {
        return Boolean.TRUE.equals(job.remotoOk());
    }

    private boolean contemPalavraJunior(String titulo) {
        return TextNormalizer.contemAlgumaPalavra(titulo, properties.getPalavrasJunior());
    }
}
