package com.devfirstdoor.domain;

import com.devfirstdoor.crawler.remoteok.RemoteOkJobMapper;
import com.devfirstdoor.util.TextNormalizer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classificações derivadas do local/fonte da vaga. Ficam persistidas na entidade
 * (e não só calculadas na resposta da API) para que os filtros da listagem rodem
 * no banco, antes da paginação.
 */
public final class ClassificacaoVaga {

    private static final Pattern LOCAL_REMOTEOK = Pattern.compile("^Remoto \\((.+)\\)$");

    /**
     * A RemoteOK agrega vagas remotas do mundo inteiro, e várias delas restringem
     * candidaturas a um país/região específico que não inclui o Brasil. Gupy e
     * ProgramaThor são boards 100% nacionais, então essa checagem só se aplica a
     * vagas da RemoteOK. Heurística por palavra-chave sobre o texto de localização
     * bruto da API (preservado dentro de "Remoto (...)" pelo RemoteOkJobMapper) —
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
     * (Gupy, ProgramaThor e RemoteOK só coletam remotas; o LinkedIn prefixa
     * as que a descrição indica como remotas).
     */
    public static boolean isRemoto(String local) {
        return TextNormalizer.normalizar(local).startsWith("remoto");
    }

    public static boolean isInternacional(String fonte, String local) {
        if (!RemoteOkJobMapper.FONTE.equals(fonte) || local == null) {
            return false;
        }
        Matcher match = LOCAL_REMOTEOK.matcher(local);
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
}
