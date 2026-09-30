package com.devfirstdoor.util;

import java.util.regex.Pattern;

/**
 * O Dev First Door só lista vagas de Java. "java" precisa estar isolado à esquerda
 * e não pode ser seguido de "script", para não confundir com JavaScript, mas
 * aceita grudado à direita ("Java8", "JavaEE", "Java/Spring").
 */
public final class LinguagemJava {

    private static final Pattern JAVA = Pattern.compile("\\bjava(?!script)", Pattern.CASE_INSENSITIVE);

    private LinguagemJava() {
    }

    public static boolean mencionadaEm(String... textos) {
        for (String texto : textos) {
            if (texto != null && JAVA.matcher(texto).find()) {
                return true;
            }
        }
        return false;
    }

    public static boolean mencionadaEm(Iterable<String> textos) {
        if (textos == null) {
            return false;
        }
        for (String texto : textos) {
            if (mencionadaEm(texto)) {
                return true;
            }
        }
        return false;
    }
}
