package com.devfirstdoor.curriculo;

import java.util.List;

/**
 * Resposta da IA na adaptação, exatamente no formato do schema enviado. Nada de empresa, cargo,
 * datas ou contato: a IA só escolhe ids e reescreve textos, sempre citando as fontes.
 */
public record AdaptacaoIa(
        ItemIa resumo,
        List<BlocoIa> experiencias,
        List<BlocoIa> projetos,
        List<String> habilidades,
        List<LacunaIa> lacunas,
        List<PerguntaIa> perguntas
) {

    public record ItemIa(String texto, List<String> fontes) {
    }

    public record BlocoIa(String id, List<BulletIa> bullets) {
    }

    public record BulletIa(String texto, List<String> fontes, List<String> termosVaga) {
    }

    public record LacunaIa(String requisitoId, String sugestao) {
    }

    public record PerguntaIa(String requisitoId, String pergunta) {
    }
}
