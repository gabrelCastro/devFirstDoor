package com.devfirstdoor.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class TextNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern ESPACOS_MULTIPLOS = Pattern.compile("\\s+");

    private TextNormalizer() {
    }

    /**
     * Minúsculas, sem acentos e com espaços colapsados — usado tanto para o hash
     * de deduplicação quanto para a busca por palavras-chave, para que variações
     * de grafia (maiúsculas, acentuação, espaçamento) não gerem falsos negativos.
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD);
        semAcento = DIACRITICS.matcher(semAcento).replaceAll("");
        String colapsado = ESPACOS_MULTIPLOS.matcher(semAcento.trim()).replaceAll(" ");
        return colapsado.toLowerCase();
    }

    /**
     * Palavras curtas (<=3 letras, como "ti", "qa", "dev", "jr") exigem borda de palavra
     * para não darem falso positivo como substring de outra palavra (ex: "ti" dentro de
     * "administrativo"). Palavras/termos maiores continuam casando por substring, o que é
     * necessário para radicais como "desenvolv" (cobre desenvolvedor, desenvolvimento etc.).
     */
    public static boolean contemAlgumaPalavra(String texto, Iterable<String> palavras) {
        String normalizado = normalizar(texto);
        for (String palavraBruta : palavras) {
            String palavra = normalizar(palavraBruta);
            if (palavra.isEmpty()) {
                continue;
            }
            if (palavra.length() <= 3) {
                if (Pattern.compile("\\b" + Pattern.quote(palavra) + "\\b").matcher(normalizado).find()) {
                    return true;
                }
            } else if (normalizado.contains(palavra)) {
                return true;
            }
        }
        return false;
    }
}
