package com.devfirstdoor.curriculo.ia;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** Falha ao falar com a IA, já com a mensagem que pode ir para o usuário (nunca a chave ou a URL). */
public class FalhaIaException extends RuntimeException {

    private final HttpStatusCode status;

    public FalhaIaException(HttpStatusCode status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public static FalhaIaException indisponivel() {
        return new FalhaIaException(HttpStatus.SERVICE_UNAVAILABLE,
                "A geração com IA está indisponível no momento. Tente de novo em alguns minutos.");
    }

    public HttpStatusCode getStatus() {
        return status;
    }
}
