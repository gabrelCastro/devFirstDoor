package com.devfirstdoor.curriculo;

import com.devfirstdoor.curriculo.AdaptacaoIa.BlocoIa;
import com.devfirstdoor.curriculo.AdaptacaoIa.BulletIa;
import com.devfirstdoor.curriculo.PerfilCurriculo.Experiencia;
import com.devfirstdoor.curriculo.PerfilCurriculo.Item;
import com.devfirstdoor.curriculo.PerfilCurriculo.Projeto;
import com.devfirstdoor.curriculo.Proposta.Bloco;
import com.devfirstdoor.curriculo.Proposta.Bullet;
import com.devfirstdoor.curriculo.Proposta.Lacuna;
import com.devfirstdoor.curriculo.Proposta.Pergunta;
import com.devfirstdoor.curriculo.Proposta.Resumo;
import com.devfirstdoor.curriculo.VagaAnalisada.Requisito;
import com.devfirstdoor.curriculo.ia.Schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Confere a resposta da IA contra o perfil antes de mostrar qualquer coisa. Um bullet só fica
 * {@code OK} se tiver fonte no próprio bloco, se toda tecnologia citada estiver nas fontes (ou nas
 * tecnologias do bloco) e se todo número existir nas fontes. O que falhar fica {@code BLOQUEADO},
 * com o motivo, e nunca entra no currículo.
 */
public final class ValidadorAdaptacao {

    static final int MAX_BULLETS_POR_BLOCO = 5;
    static final int MAX_TAMANHO_BULLET = 300;
    static final int MAX_TAMANHO_RESUMO = 700;
    static final int MAX_LACUNAS = 8;
    static final int MAX_PERGUNTAS = 5;

    private static final Pattern NUMERO = Pattern.compile("\\d+(?:[.,]\\d+)*");

    private ValidadorAdaptacao() {
    }

    public static Proposta validar(PerfilCurriculo perfil, VagaAnalisada vaga, AdaptacaoIa ia) {
        Map<String, Experiencia> experiencias = porId(perfil.experiencias(), Experiencia::id);
        Map<String, Projeto> projetos = porId(perfil.projetos(), Projeto::id);

        List<Bloco> blocosExp = blocos(ia.experiencias(), experiencias,
                e -> new Fatos(e.id(), e.cargo() + " · " + e.empresa(), e.bullets(), e.tecnologias()));
        List<Bloco> blocosProj = blocos(ia.projetos(), projetos,
                p -> new Fatos(p.id(), p.nome(), p.bullets(), p.tecnologias()));

        Map<String, Requisito> requisitos = porId(vaga.requisitos(), Requisito::id);
        List<Lacuna> lacunas = nulo(ia.lacunas()).stream()
                .filter(l -> l != null && requisitos.containsKey(l.requisitoId()) && preenchido(l.sugestao()))
                .limit(MAX_LACUNAS)
                .map(l -> new Lacuna(l.requisitoId(), requisitos.get(l.requisitoId()).texto(), l.sugestao().strip()))
                .toList();
        List<Pergunta> perguntas = nulo(ia.perguntas()).stream()
                .filter(p -> p != null && requisitos.containsKey(p.requisitoId()) && preenchido(p.pergunta()))
                .limit(MAX_PERGUNTAS)
                .map(p -> new Pergunta(p.requisitoId(), requisitos.get(p.requisitoId()).texto(), p.pergunta().strip()))
                .toList();

        return new Proposta(resumo(perfil, ia.resumo()), blocosExp, blocosProj, habilidades(perfil, ia.habilidades()),
                lacunas, perguntas, null);
    }

    private record Fatos(String id, String titulo, List<Item> bullets, List<String> tecnologias) {
    }

    private static <T> List<Bloco> blocos(List<BlocoIa> propostos, Map<String, T> existentes, Function<T, Fatos> fatos) {
        Map<String, List<BulletIa>> porBloco = new LinkedHashMap<>();
        for (BlocoIa bloco : nulo(propostos)) {
            if (bloco != null && existentes.containsKey(bloco.id())) {
                porBloco.computeIfAbsent(bloco.id(), id -> new ArrayList<>()).addAll(nulo(bloco.bullets()));
            }
        }
        List<Bloco> resultado = new ArrayList<>();
        porBloco.forEach((id, bullets) -> {
            Fatos f = fatos.apply(existentes.get(id));
            List<Bullet> validados = new ArrayList<>();
            for (BulletIa b : bullets) {
                if (b == null || !preenchido(b.texto()) || validados.size() >= MAX_BULLETS_POR_BLOCO) continue;
                validados.add(bullet(id + ":" + validados.size(), b, f));
            }
            resultado.add(new Bloco(id, f.titulo(), validados));
        });
        return resultado;
    }

    private static Bullet bullet(String chave, BulletIa proposto, Fatos bloco) {
        String texto = proposto.texto().strip();
        Map<String, String> textosDoBloco = bloco.bullets().stream()
                .collect(Collectors.toMap(Item::id, Item::texto, (a, b) -> a, LinkedHashMap::new));

        // Fonte válida: um bullet deste bloco, ou o próprio bloco (vale pelas tecnologias e todos os tópicos).
        List<String> fontes = new ArrayList<>();
        List<String> originais = new ArrayList<>();
        for (String fonte : new LinkedHashSet<>(nulo(proposto.fontes()))) {
            if (textosDoBloco.containsKey(fonte)) {
                fontes.add(fonte);
                originais.add(textosDoBloco.get(fonte));
            } else if (bloco.id().equals(fonte)) {
                fontes.add(fonte);
            }
        }
        String baseFontes = fontes.contains(bloco.id())
                ? String.join("\n", textosDoBloco.values())
                : String.join("\n", originais);
        List<String> termosVaga = nulo(proposto.termosVaga()).stream()
                .filter(t -> preenchido(t) && Termos.contem(texto, t)).distinct().toList();

        String motivo = null;
        if (fontes.isEmpty()) {
            motivo = "Não aponta para nenhum tópico desta experiência no seu perfil.";
        } else if (texto.length() > MAX_TAMANHO_BULLET) {
            motivo = "Ficou longo demais para um tópico.";
        } else {
            Set<String> permitidas = new HashSet<>(Termos.encontrar(baseFontes));
            bloco.tecnologias().forEach(t -> permitidas.add(Termos.canonico(t)));
            Set<String> inventadas = new LinkedHashSet<>(Termos.encontrar(texto));
            inventadas.removeAll(permitidas);
            List<String> numerosNovos = numerosAusentes(texto, baseFontes);
            if (!inventadas.isEmpty()) {
                motivo = "Cita " + String.join(", ", inventadas) + ", que não aparece nas fontes deste tópico.";
            } else if (!numerosNovos.isEmpty()) {
                motivo = "Traz o número " + numerosNovos.get(0) + ", que não está no seu perfil.";
            }
        }
        return new Bullet(chave, texto, fontes, originais, termosVaga,
                motivo == null ? Proposta.OK : Proposta.BLOQUEADO, motivo);
    }

    /** O resumo pode usar o perfil inteiro, mas também não inventa tecnologia nem número. */
    private static Resumo resumo(PerfilCurriculo perfil, AdaptacaoIa.ItemIa proposto) {
        String original = perfil.resumo();
        if (proposto == null || !preenchido(proposto.texto())) {
            return new Resumo(null, original, Proposta.BLOQUEADO, "A IA não sugeriu um resumo.");
        }
        String texto = proposto.texto().strip();
        String corpus = textoDoPerfil(perfil);
        Set<String> permitidas = new HashSet<>(Termos.encontrar(corpus));
        Stream.concat(perfil.habilidades().stream(), tecnologias(perfil).stream())
                .forEach(t -> permitidas.add(Termos.canonico(t)));
        Set<String> inventadas = new LinkedHashSet<>(Termos.encontrar(texto));
        inventadas.removeAll(permitidas);
        List<String> numerosNovos = numerosAusentes(texto, corpus);
        String motivo = null;
        if (texto.length() > MAX_TAMANHO_RESUMO) {
            motivo = "O resumo ficou longo demais.";
        } else if (!inventadas.isEmpty()) {
            motivo = "Cita " + String.join(", ", inventadas) + ", que não aparece no seu perfil.";
        } else if (!numerosNovos.isEmpty()) {
            motivo = "Traz o número " + numerosNovos.get(0) + ", que não está no seu perfil.";
        }
        return new Resumo(texto, original, motivo == null ? Proposta.OK : Proposta.BLOQUEADO, motivo);
    }

    /** Só habilidades que existem no perfil (lista de habilidades ou tecnologias), na grafia do perfil. */
    private static List<String> habilidades(PerfilCurriculo perfil, List<String> propostas) {
        Map<String, String> doPerfil = new LinkedHashMap<>();
        Stream.concat(perfil.habilidades().stream(), tecnologias(perfil).stream())
                .forEach(h -> doPerfil.putIfAbsent(Termos.canonico(h), h));
        Set<String> escolhidas = new LinkedHashSet<>();
        for (String h : nulo(propostas)) {
            if (h != null && !Schema.SEM_OPCOES.equals(h)) {
                String doPerfilNaGrafia = doPerfil.get(Termos.canonico(h));
                if (doPerfilNaGrafia != null) escolhidas.add(doPerfilNaGrafia);
            }
        }
        return List.copyOf(escolhidas);
    }

    /** Números do texto que não aparecem na base ("1.000" e "1000" contam como o mesmo). */
    static List<String> numerosAusentes(String texto, String base) {
        Set<String> daBase = new HashSet<>();
        Matcher mb = NUMERO.matcher(base);
        while (mb.find()) daBase.add(mb.group().replaceAll("[.,]", ""));
        List<String> ausentes = new ArrayList<>();
        Matcher mt = NUMERO.matcher(texto);
        while (mt.find()) {
            if (!daBase.contains(mt.group().replaceAll("[.,]", ""))) ausentes.add(mt.group());
        }
        return ausentes;
    }

    static List<String> tecnologias(PerfilCurriculo perfil) {
        return Stream.concat(perfil.experiencias().stream().flatMap(e -> e.tecnologias().stream()),
                perfil.projetos().stream().flatMap(p -> p.tecnologias().stream())).toList();
    }

    static String textoDoPerfil(PerfilCurriculo perfil) {
        List<String> partes = new ArrayList<>();
        if (perfil.resumo() != null) partes.add(perfil.resumo());
        perfil.experiencias().forEach(e -> {
            partes.add(e.cargo() + " " + e.empresa());
            e.bullets().forEach(b -> partes.add(b.texto()));
            partes.addAll(e.tecnologias());
        });
        perfil.projetos().forEach(p -> {
            partes.add(p.nome());
            p.bullets().forEach(b -> partes.add(b.texto()));
            partes.addAll(p.tecnologias());
        });
        perfil.formacoes().forEach(f -> partes.add(f.curso() + " " + f.instituicao()));
        perfil.cursos().forEach(c -> partes.add(c.nome()));
        perfil.idiomas().forEach(i -> partes.add(i.idioma() + " " + (i.nivel() == null ? "" : i.nivel())));
        partes.addAll(perfil.habilidades());
        return String.join("\n", partes);
    }

    private static <T> Map<String, T> porId(List<T> itens, Function<T, String> id) {
        Map<String, T> mapa = new LinkedHashMap<>();
        itens.forEach(item -> mapa.put(id.apply(item), item));
        return mapa;
    }

    private static boolean preenchido(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static <T> List<T> nulo(List<T> lista) {
        return lista == null ? List.of() : lista;
    }
}
