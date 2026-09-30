package com.devfirstdoor.crawler.greenhouse;

import com.devfirstdoor.crawler.greenhouse.dto.GreenhouseJobDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.util.LinguagemJava;
import com.devfirstdoor.util.TextNormalizer;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

import java.util.Optional;

public class GreenhouseVagaClassifier {

    private final GreenhouseCrawlerProperties properties;

    public GreenhouseVagaClassifier(GreenhouseCrawlerProperties properties) {
        this.properties = properties;
    }

    public Optional<NivelVaga> classificarNivel(GreenhouseJobDto job) {
        String titulo = job.title();
        if (TextNormalizer.contemAlgumaPalavra(titulo, properties.getPalavrasEstagio())) {
            return Optional.of(NivelVaga.ESTAGIO);
        }
        if (TextNormalizer.contemAlgumaPalavra(titulo, properties.getPalavrasJunior())) {
            return Optional.of(NivelVaga.JUNIOR);
        }
        return Optional.empty();
    }

    public boolean isRelevanteParaTech(GreenhouseJobDto job) {
        return TextNormalizer.contemAlgumaPalavra(job.title(), properties.getPalavrasTech());
    }

    public boolean isRemota(GreenhouseJobDto job) {
        return TextNormalizer.contemAlgumaPalavra(job.nomeDoLocal(), properties.getPalavrasRemoto());
    }

    public boolean isJava(GreenhouseJobDto job) {
        return LinguagemJava.mencionadaEm(job.title(), textoDoConteudo(job.content()));
    }

    /** O "content" vem com o HTML escapado: desescapa e tira as tags antes de procurar Java. */
    static String textoDoConteudo(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        return Jsoup.parse(Parser.unescapeEntities(content, false)).text();
    }
}
