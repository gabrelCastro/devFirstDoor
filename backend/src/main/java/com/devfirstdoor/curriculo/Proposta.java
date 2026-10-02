package com.devfirstdoor.curriculo;

import java.util.List;

/**
 * A adaptação já validada, como é mostrada para a pessoa aprovar. Cada bullet traz os textos
 * originais das fontes, para o diff, e o status: {@code OK} pode entrar no currículo;
 * {@code BLOQUEADO} foi barrado pelo validador (citou algo que não está no perfil) e nunca entra.
 */
public record Proposta(
        Resumo resumo,
        List<Bloco> experiencias,
        List<Bloco> projetos,
        List<String> habilidades,
        List<Lacuna> lacunas,
        List<Pergunta> perguntas,
        Cobertura cobertura
) {

    public static final String OK = "OK";
    public static final String BLOQUEADO = "BLOQUEADO";

    public record Resumo(String proposto, String original, String status, String motivo) {
    }

    public record Bloco(String id, String titulo, List<Bullet> bullets) {
    }

    public record Bullet(String chave, String texto, List<String> fontes, List<String> originais,
                         List<String> termosVaga, String status, String motivo) {
    }

    public record Lacuna(String requisitoId, String requisito, String sugestao) {
    }

    public record Pergunta(String requisitoId, String requisito, String pergunta) {
    }

    /** Palavras-chave da vaga presentes no currículo antes (perfil puro) e depois (adaptado). */
    public record Cobertura(List<String> palavrasChave, List<String> antes, List<String> depois) {
    }

    public Proposta comCobertura(Cobertura nova) {
        return new Proposta(resumo, experiencias, projetos, habilidades, lacunas, perguntas, nova);
    }
}
