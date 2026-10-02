package com.devfirstdoor.service;

/** A vaga já é acompanhada: o controller responde 409 com o id existente, para o front abrir ela. */
public class CandidaturaDuplicadaException extends RuntimeException {

    private final long candidaturaId;

    public CandidaturaDuplicadaException(long candidaturaId) {
        super("Você já acompanha esta vaga");
        this.candidaturaId = candidaturaId;
    }

    public long getCandidaturaId() {
        return candidaturaId;
    }
}
