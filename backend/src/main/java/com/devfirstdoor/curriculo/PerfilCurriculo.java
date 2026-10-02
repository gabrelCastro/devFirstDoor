package com.devfirstdoor.curriculo;

import java.util.List;

/**
 * Perfil-mestre do currículo: os fatos da pessoa, preenchidos uma vez. Toda experiência, projeto
 * e bullet tem um id estável; a adaptação por IA só aponta para esses ids, então não tem como
 * citar algo que não está aqui.
 *
 * Datas no formato {@code aaaa-mm}; {@code fim} nulo quer dizer "atual".
 */
public record PerfilCurriculo(
        Contato contato,
        String resumo,
        List<Experiencia> experiencias,
        List<Projeto> projetos,
        List<Formacao> formacoes,
        List<Curso> cursos,
        List<String> habilidades,
        List<Idioma> idiomas
) {

    public record Contato(String nome, String email, String telefone, String cidade,
                          String linkedin, String github, String portfolio) {
    }

    public record Item(String id, String texto) {
    }

    public record Experiencia(String id, String cargo, String empresa, String local, String inicio, String fim,
                              List<Item> bullets, List<String> tecnologias) {
    }

    public record Projeto(String id, String nome, String link, List<Item> bullets, List<String> tecnologias) {
    }

    public record Formacao(String id, String curso, String instituicao, String inicio, String fim, String situacao) {
    }

    public record Curso(String id, String nome, String instituicao, String ano) {
    }

    public record Idioma(String id, String idioma, String nivel) {
    }

    public static PerfilCurriculo vazio() {
        return new PerfilCurriculo(new Contato(null, null, null, null, null, null, null), null,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
