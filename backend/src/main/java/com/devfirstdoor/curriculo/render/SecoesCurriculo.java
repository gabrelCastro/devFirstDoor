package com.devfirstdoor.curriculo.render;

import com.devfirstdoor.curriculo.CurriculoFinal;
import com.devfirstdoor.curriculo.CurriculoFinal.Secao;
import com.devfirstdoor.curriculo.PerfilCurriculo.Contato;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Ordem e títulos das seções, iguais no PDF e no DOCX. Títulos padrão em português ("Experiência",
 * "Formação"...) são os que os ATS reconhecem; nada de colunas, tabelas ou ícones.
 */
final class SecoesCurriculo {

    record Bloco(String titulo, List<Secao> itens, List<String> linhas) {
    }

    private SecoesCurriculo() {
    }

    static List<Bloco> blocos(CurriculoFinal c) {
        List<Bloco> blocos = new ArrayList<>();
        if (c.resumo() != null && !c.resumo().isBlank()) blocos.add(new Bloco("Resumo", List.of(), List.of(c.resumo())));
        Bloco experiencia = new Bloco("Experiência", c.experiencias(), List.of());
        Bloco projetos = new Bloco("Projetos", c.projetos(), List.of());
        for (Bloco b : c.projetosPrimeiro() ? List.of(projetos, experiencia) : List.of(experiencia, projetos)) {
            if (!b.itens().isEmpty()) blocos.add(b);
        }
        if (!c.formacoes().isEmpty()) blocos.add(new Bloco("Formação", List.of(), c.formacoes()));
        if (!c.cursos().isEmpty()) blocos.add(new Bloco("Cursos e certificações", List.of(), c.cursos()));
        if (!c.habilidades().isEmpty()) blocos.add(new Bloco("Habilidades", List.of(), List.of(String.join(", ", c.habilidades()))));
        if (!c.idiomas().isEmpty()) blocos.add(new Bloco("Idiomas", List.of(), c.idiomas()));
        return blocos;
    }

    /** E-mail, telefone, cidade e links numa linha, separados por " | ". */
    static String linhaContato(Contato contato) {
        if (contato == null) return "";
        return String.join(" | ", Stream.of(contato.email(), contato.telefone(), contato.cidade(),
                contato.linkedin(), contato.github(), contato.portfolio())
                .filter(v -> v != null && !v.isBlank()).toList());
    }

    /** "Empresa · Local | 02/2025 – atual" (ou só o que existir). */
    static String linhaApoio(Secao s) {
        return String.join(" | ", Stream.of(s.subtitulo(), s.periodo()).filter(v -> v != null && !v.isBlank()).toList());
    }
}
