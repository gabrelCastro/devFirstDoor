package com.devfirstdoor.controller.dto;

import com.devfirstdoor.crawler.remoteok.RemoteOkJobMapper;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.util.TextNormalizer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record VagaResponse(
        Long id,
        String titulo,
        String empresa,
        String local,
        NivelVaga nivel,
        String link,
        String fonte,
        LocalDate dataPublicacao,
        LocalDateTime dataColeta,
        boolean internacional,
        boolean remoto
) {
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

    public static VagaResponse from(Vaga vaga) {
        return new VagaResponse(
                vaga.getId(),
                vaga.getTitulo(),
                vaga.getEmpresa(),
                vaga.getLocal(),
                vaga.getNivel(),
                vaga.getLink(),
                vaga.getFonte(),
                vaga.getDataPublicacao(),
                vaga.getDataColeta(),
                isInternacional(vaga),
                isRemoto(vaga)
        );
    }

    /**
     * Todas as fontes marcam vagas remotas com o prefixo "Remoto" no local
     * (Gupy, ProgramaThor e RemoteOK só coletam remotas; o LinkedIn prefixa
     * as que a descrição indica como remotas).
     */
    static boolean isRemoto(Vaga vaga) {
        return TextNormalizer.normalizar(vaga.getLocal()).startsWith("remoto");
    }

    static boolean isInternacional(Vaga vaga) {
        if (!RemoteOkJobMapper.FONTE.equals(vaga.getFonte()) || vaga.getLocal() == null) {
            return false;
        }
        Matcher match = LOCAL_REMOTEOK.matcher(vaga.getLocal());
        if (!match.matches()) {
            return false;
        }
        return TextNormalizer.contemAlgumaPalavra(match.group(1), PALAVRAS_RESTRICAO_GEOGRAFICA);
    }
}
