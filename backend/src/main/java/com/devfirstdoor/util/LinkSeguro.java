package com.devfirstdoor.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Locale;

/** Links digitados pelo usuário viram href na página e no PDF: só http(s) com host passa. */
public final class LinkSeguro {

    private LinkSeguro() {
    }

    /** Nulo ou em branco devolve nulo; inválido responde 400 citando o campo. */
    public static String validar(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpo = valor.strip();
        try {
            URI uri = URI.create(limpo);
            String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ((esquema.equals("http") || esquema.equals("https")) && uri.getHost() != null) {
                return limpo;
            }
        } catch (IllegalArgumentException e) {
            // cai no erro abaixo
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " deve começar com http:// ou https://");
    }
}
