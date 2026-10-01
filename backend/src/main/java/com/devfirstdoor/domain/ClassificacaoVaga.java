package com.devfirstdoor.domain;

import com.devfirstdoor.crawler.greenhouse.GreenhouseJobMapper;
import com.devfirstdoor.crawler.lever.LeverJobMapper;
import com.devfirstdoor.crawler.remoteok.RemoteOkJobMapper;
import com.devfirstdoor.util.TextNormalizer;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classificações derivadas do local/fonte da vaga. Ficam persistidas na entidade
 * (e não só calculadas na resposta da API) para que os filtros da listagem rodem
 * no banco, antes da paginação.
 */
public final class ClassificacaoVaga {

    private static final Pattern LOCAL_COM_TEXTO_DA_FONTE = Pattern.compile("^Remoto \\((.+)\\)$");

    /** Fontes com vagas do mundo inteiro, em que o local pode restringir a outro país. */
    private static final Set<String> FONTES_GLOBAIS = Set.of(
            RemoteOkJobMapper.FONTE, GreenhouseJobMapper.FONTE, LeverJobMapper.FONTE);

    /**
     * A RemoteOK (e as empresas do Greenhouse e do Lever) têm vagas remotas do mundo
     * inteiro, e várias delas restringem candidaturas a um país/região específico que
     * não inclui o Brasil. Gupy e ProgramaThor são boards 100% nacionais, então essa
     * checagem só se aplica às FONTES_GLOBAIS. Heurística por palavra-chave sobre o
     * texto de localização bruto da API (preservado dentro de "Remoto (...)" pelos mappers) —
     * na dúvida (texto vazio, "Worldwide"/"Anywhere", ou algo não reconhecido) a
     * vaga é considerada aberta, para não esconder oportunidades por engano.
     */
    private static final List<String> PALAVRAS_RESTRICAO_GEOGRAFICA = List.of(
            "usa", "us only", "u.s.", "united states", "us-based",
            "uk", "united kingdom",
            "canada",
            "europe", "eu only", "emea",
            "apac", "australia", "new zealand",
            "north america",
            "germany", "netherlands", "france", "spain", "italy", "poland", "ireland", "portugal",
            "india", "singapore", "japan"
    );

    private ClassificacaoVaga() {
    }

    /**
     * Todas as fontes marcam vagas remotas com o prefixo "Remoto" no local
     * (Gupy, ProgramaThor, RemoteOK, Greenhouse e Lever só coletam remotas; o LinkedIn prefixa
     * as que a descrição indica como remotas).
     */
    public static boolean isRemoto(String local) {
        return TextNormalizer.normalizar(local).startsWith("remoto");
    }

    public static boolean isInternacional(String fonte, String local) {
        if (!FONTES_GLOBAIS.contains(fonte) || local == null) {
            return false;
        }
        Matcher match = LOCAL_COM_TEXTO_DA_FONTE.matcher(local);
        if (!match.matches()) {
            return false;
        }
        return TextNormalizer.contemAlgumaPalavra(match.group(1), PALAVRAS_RESTRICAO_GEOGRAFICA);
    }

    /**
     * Texto já normalizado (sem acento, minúsculo) em que a busca da API procura,
     * porque ignorar acentos direto no SQL não é portável entre H2 e PostgreSQL.
     */
    public static String textoDeBusca(String titulo, String empresa, String local) {
        return TextNormalizer.normalizar(titulo + " " + empresa + " " + (local == null ? "" : local));
    }

    /** Mesma normalização usada pela moderação para reconhecer vagas iguais entre fontes. */
    public static String normalizarParaDuplicata(String valor) {
        return TextNormalizer.normalizar(valor);
    }
}
