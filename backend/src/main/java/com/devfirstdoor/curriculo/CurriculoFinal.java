package com.devfirstdoor.curriculo;

import com.devfirstdoor.curriculo.PerfilCurriculo.Contato;
import com.devfirstdoor.curriculo.PerfilCurriculo.Experiencia;
import com.devfirstdoor.curriculo.PerfilCurriculo.Item;
import com.devfirstdoor.curriculo.PerfilCurriculo.Projeto;
import com.devfirstdoor.curriculo.Proposta.Bloco;
import com.devfirstdoor.curriculo.Proposta.Bullet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * O currículo pronto para virar PDF/DOCX/texto: fatos sempre do perfil, textos da proposta só
 * quando aceitos e válidos. Bullet recusado ou bloqueado volta ao texto original das fontes.
 */
public record CurriculoFinal(
        Contato contato,
        String resumo,
        List<Secao> experiencias,
        List<Secao> projetos,
        List<String> formacoes,
        List<String> cursos,
        List<String> habilidades,
        List<String> idiomas
) {

    /** Uma experiência ou projeto já formatado: título, linha de apoio, período e tópicos. */
    public record Secao(String titulo, String subtitulo, String periodo, List<String> bullets, List<String> tecnologias) {
    }

    /** Estudante sem experiência: projetos vêm antes, que é onde está o que ele já fez. */
    public boolean projetosPrimeiro() {
        return experiencias.isEmpty() && !projetos.isEmpty();
    }

    /** Currículo só com o perfil, sem adaptação. */
    public static CurriculoFinal doPerfil(PerfilCurriculo perfil) {
        return montar(perfil, perfil.contato(), null, null);
    }

    public static CurriculoFinal montar(PerfilCurriculo perfil, Contato contato, Proposta proposta, Escolhas escolhas) {
        Escolhas e = escolhas == null ? Escolhas.aceitarTudo() : escolhas;
        Set<String> recusados = new LinkedHashSet<>(e.recusados());

        String resumo = perfil.resumo();
        if (proposta != null && e.usarResumo() && proposta.resumo() != null
                && Proposta.OK.equals(proposta.resumo().status()) && proposta.resumo().proposto() != null) {
            resumo = proposta.resumo().proposto();
        }

        Map<String, Experiencia> exps = perfil.experiencias().stream()
                .collect(Collectors.toMap(Experiencia::id, x -> x, (a, b) -> a, LinkedHashMap::new));
        Map<String, Projeto> projs = perfil.projetos().stream()
                .collect(Collectors.toMap(Projeto::id, x -> x, (a, b) -> a, LinkedHashMap::new));

        // Experiências: as escolhidas pela IA, na ordem dela, e depois as demais (nenhuma some do currículo).
        List<Secao> experiencias = new ArrayList<>();
        Set<String> usadas = new LinkedHashSet<>();
        if (proposta != null) {
            for (Bloco bloco : proposta.experiencias()) {
                Experiencia exp = exps.get(bloco.id());
                if (exp != null && usadas.add(exp.id())) {
                    experiencias.add(secao(exp, bullets(bloco, exp.bullets(), recusados)));
                }
            }
        }
        exps.values().stream().filter(x -> !usadas.contains(x.id()))
                .forEach(x -> experiencias.add(secao(x, textos(x.bullets()))));

        // Projetos: só os escolhidos pela IA (se ela escolheu algum); senão, todos.
        List<Secao> projetos = new ArrayList<>();
        if (proposta != null && !proposta.projetos().isEmpty()) {
            Set<String> projUsados = new LinkedHashSet<>();
            for (Bloco bloco : proposta.projetos()) {
                Projeto p = projs.get(bloco.id());
                if (p != null && projUsados.add(p.id())) {
                    projetos.add(secao(p, bullets(bloco, p.bullets(), recusados)));
                }
            }
        } else {
            projs.values().forEach(p -> projetos.add(secao(p, textos(p.bullets()))));
        }

        // Habilidades: as mais relevantes para a vaga primeiro, depois o restante do perfil.
        Map<String, String> habilidades = new LinkedHashMap<>();
        if (proposta != null) {
            proposta.habilidades().forEach(h -> habilidades.putIfAbsent(Termos.canonico(h), h));
        }
        perfil.habilidades().forEach(h -> habilidades.putIfAbsent(Termos.canonico(h), h));

        List<String> formacoes = perfil.formacoes().stream().map(f -> {
            String situacao = f.situacao() == null ? null : switch (f.situacao()) {
                case "CURSANDO" -> "cursando";
                case "TRANCADO" -> "trancado";
                default -> "concluído";
            };
            StringBuilder linha = new StringBuilder(f.curso()).append(" — ").append(f.instituicao());
            String periodo = periodo(f.inicio(), f.fim(), situacao);
            if (!periodo.isEmpty()) linha.append(" (").append(periodo).append(")");
            return linha.toString();
        }).toList();
        List<String> cursos = perfil.cursos().stream().map(c -> c.nome()
                + (c.instituicao() == null ? "" : " — " + c.instituicao())
                + (c.ano() == null ? "" : " (" + c.ano() + ")")).toList();
        List<String> idiomas = perfil.idiomas().stream()
                .map(i -> i.idioma() + (i.nivel() == null ? "" : " — " + i.nivel())).toList();

        return new CurriculoFinal(contato == null ? perfil.contato() : contato, resumo, experiencias, projetos,
                formacoes, cursos, List.copyOf(habilidades.values()), idiomas);
    }

    private static List<String> bullets(Bloco bloco, List<Item> originais, Set<String> recusados) {
        // Fontes já reescritas por um tópico aceito: o original delas não volta, senão o mesmo
        // fato apareceria duas vezes (reescrito e original).
        Set<String> jaUsados = new LinkedHashSet<>();
        for (Bullet b : bloco.bullets()) {
            if (aceito(b, recusados)) jaUsados.addAll(b.originais());
        }
        List<String> resultado = new ArrayList<>();
        for (Bullet b : bloco.bullets()) {
            if (aceito(b, recusados)) {
                resultado.add(b.texto());
            } else {
                // Recusado ou bloqueado: entram os originais das fontes que ninguém cobriu, sem repetir.
                for (String original : b.originais()) {
                    if (jaUsados.add(original)) resultado.add(original);
                }
            }
        }
        return resultado.isEmpty() ? textos(originais) : resultado;
    }

    private static boolean aceito(Bullet b, Set<String> recusados) {
        return Proposta.OK.equals(b.status()) && !recusados.contains(b.chave());
    }

    private static Secao secao(Experiencia e, List<String> bullets) {
        return new Secao(e.cargo(), e.empresa() + (e.local() == null ? "" : " · " + e.local()),
                periodo(e.inicio(), e.fim(), e.fim() == null && e.inicio() != null ? "atual" : null),
                bullets, e.tecnologias());
    }

    private static Secao secao(Projeto p, List<String> bullets) {
        return new Secao(p.nome(), p.link(), "", bullets, p.tecnologias());
    }

    private static List<String> textos(List<Item> itens) {
        return itens.stream().map(Item::texto).toList();
    }

    /** "02/2024 – 06/2025", "02/2024 – atual", ou vazio. */
    static String periodo(String inicio, String fim, String semFim) {
        String i = mesAno(inicio);
        String f = fim != null ? mesAno(fim) : semFim;
        if (i == null && f == null) return "";
        if (i == null) return f;
        return f == null ? i : i + " – " + f;
    }

    private static String mesAno(String data) {
        if (data == null || data.length() != 7) return null;
        return data.substring(5) + "/" + data.substring(0, 4);
    }

    /** Texto corrido do currículo inteiro (cobertura de palavras-chave e exportação em texto). */
    public String comoTexto() {
        List<String> linhas = new ArrayList<>();
        if (contato != null && contato.nome() != null) linhas.add(contato.nome());
        if (resumo != null) linhas.add(resumo);
        for (Secao s : projetosPrimeiro() ? concat(projetos, experiencias) : concat(experiencias, projetos)) {
            linhas.add(s.titulo());
            if (s.subtitulo() != null) linhas.add(s.subtitulo());
            linhas.addAll(s.bullets());
            if (!s.tecnologias().isEmpty()) linhas.add(String.join(", ", s.tecnologias()));
        }
        linhas.addAll(formacoes);
        linhas.addAll(cursos);
        linhas.add(String.join(", ", habilidades));
        linhas.addAll(idiomas);
        return String.join("\n", linhas);
    }

    private static List<Secao> concat(List<Secao> a, List<Secao> b) {
        List<Secao> todas = new ArrayList<>(a);
        todas.addAll(b);
        return todas;
    }
}
