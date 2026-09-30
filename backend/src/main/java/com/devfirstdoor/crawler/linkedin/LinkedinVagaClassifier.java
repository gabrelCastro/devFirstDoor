package com.devfirstdoor.crawler.linkedin;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.util.TextNormalizer;

import java.util.List;
import java.util.Optional;

/**
 * A busca pública do LinkedIn ignora o filtro de nível de experiência e mistura
 * vagas pleno/sênior mesmo em buscas por "júnior", então nível e relevância para
 * tecnologia são decididos pelo título.
 *
 * A modalidade também não é filtrável nem vem como campo estruturado: é deduzida do
 * título + descrição. A ordem das checagens importa porque descrições híbridas citam
 * "remoto" e "presencial" ao mesmo tempo ("3 dias presenciais, 2 remotos"), e vagas
 * remotas às vezes citam encontros presenciais.
 */
public class LinkedinVagaClassifier {

    private static final List<String> EXPLICITO_REMOTO = List.of(
            "100% remot", "totalmente remot", "full remote", "fully remote", "100% home office");
    private static final List<String> EXPLICITO_PRESENCIAL = List.of("100% presencial", "totalmente presencial");
    private static final List<String> HIBRIDO = List.of("hibrid", "hybrid");
    private static final List<String> REMOTO = List.of("remoto", "remota", "home office", "home-office", "remote");
    private static final List<String> PRESENCIAL = List.of("presencial", "on-site", "onsite");

    private final LinkedinCrawlerProperties properties;

    public LinkedinVagaClassifier(LinkedinCrawlerProperties properties) {
        this.properties = properties;
    }

    public Optional<NivelVaga> classificarNivel(LinkedinJobDto job) {
        if (TextNormalizer.contemAlgumaPalavra(job.titulo(), properties.getPalavrasEstagio())) {
            return Optional.of(NivelVaga.ESTAGIO);
        }
        if (TextNormalizer.contemAlgumaPalavra(job.titulo(), properties.getPalavrasJunior())) {
            return Optional.of(NivelVaga.JUNIOR);
        }
        return Optional.empty();
    }

    public LinkedinModalidade classificarModalidade(String titulo, String descricao) {
        String texto = (titulo != null ? titulo : "") + " " + (descricao != null ? descricao : "");
        if (TextNormalizer.contemAlgumaPalavra(texto, EXPLICITO_REMOTO)) {
            return LinkedinModalidade.REMOTO;
        }
        if (TextNormalizer.contemAlgumaPalavra(texto, EXPLICITO_PRESENCIAL)) {
            return LinkedinModalidade.PRESENCIAL;
        }
        if (TextNormalizer.contemAlgumaPalavra(texto, HIBRIDO)) {
            return LinkedinModalidade.HIBRIDO;
        }
        if (TextNormalizer.contemAlgumaPalavra(texto, REMOTO)) {
            return LinkedinModalidade.REMOTO;
        }
        if (TextNormalizer.contemAlgumaPalavra(texto, PRESENCIAL)) {
            return LinkedinModalidade.PRESENCIAL;
        }
        return LinkedinModalidade.DESCONHECIDA;
    }

    public boolean isRelevanteParaTech(LinkedinJobDto job) {
        return TextNormalizer.contemAlgumaPalavra(job.titulo(), properties.getPalavrasTech());
    }
}
