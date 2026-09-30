package com.devfirstdoor.crawler.lever;

import com.devfirstdoor.crawler.lever.dto.LeverListDto;
import com.devfirstdoor.crawler.lever.dto.LeverPostingDto;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.util.LinguagemJava;
import com.devfirstdoor.util.TextNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LeverVagaClassifier {

    private final LeverCrawlerProperties properties;

    public LeverVagaClassifier(LeverCrawlerProperties properties) {
        this.properties = properties;
    }

    public Optional<NivelVaga> classificarNivel(LeverPostingDto posting) {
        String titulo = posting.text();
        if (TextNormalizer.contemAlgumaPalavra(titulo, properties.getPalavrasEstagio())) {
            return Optional.of(NivelVaga.ESTAGIO);
        }
        if (TextNormalizer.contemAlgumaPalavra(titulo, properties.getPalavrasJunior())) {
            return Optional.of(NivelVaga.JUNIOR);
        }
        return Optional.empty();
    }

    public boolean isRelevanteParaTech(LeverPostingDto posting) {
        return TextNormalizer.contemAlgumaPalavra(posting.text(), properties.getPalavrasTech());
    }

    /**
     * A modalidade declarada vale mais que o local: "Remote" no local de uma vaga
     * "hybrid" costuma ser só o nome do escritório. O local só decide quando a
     * empresa não preencheu a modalidade.
     */
    public boolean isRemota(LeverPostingDto posting) {
        String modalidade = posting.workplaceType();
        if (modalidade != null && !modalidade.isBlank() && !modalidade.equalsIgnoreCase("unspecified")) {
            return modalidade.equalsIgnoreCase("remote");
        }
        return TextNormalizer.contemAlgumaPalavra(posting.local(), properties.getPalavrasRemoto());
    }

    public boolean isJava(LeverPostingDto posting) {
        List<String> textos = new ArrayList<>();
        textos.add(posting.text());
        textos.add(posting.descriptionPlain());
        textos.add(posting.additionalPlain());
        if (posting.lists() != null) {
            for (LeverListDto lista : posting.lists()) {
                textos.add(lista.content());
            }
        }
        return LinguagemJava.mencionadaEm(textos);
    }
}
