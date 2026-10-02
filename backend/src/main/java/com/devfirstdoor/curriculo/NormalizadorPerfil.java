package com.devfirstdoor.curriculo;

import com.devfirstdoor.curriculo.PerfilCurriculo.Contato;
import com.devfirstdoor.curriculo.PerfilCurriculo.Curso;
import com.devfirstdoor.curriculo.PerfilCurriculo.Experiencia;
import com.devfirstdoor.curriculo.PerfilCurriculo.Formacao;
import com.devfirstdoor.curriculo.PerfilCurriculo.Idioma;
import com.devfirstdoor.curriculo.PerfilCurriculo.Item;
import com.devfirstdoor.curriculo.PerfilCurriculo.Projeto;
import com.devfirstdoor.util.LinkSeguro;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Limpa e valida o perfil antes de salvar: tira espaços, descarta bullets vazios, remove
 * habilidades repetidas, confere datas e links e garante que todo item tenha um id único.
 * Ids já existentes são mantidos (a adaptação e as versões apontam para eles).
 */
public final class NormalizadorPerfil {

    static final int MAX_EXPERIENCIAS = 15;
    static final int MAX_PROJETOS = 15;
    static final int MAX_FORMACOES = 8;
    static final int MAX_CURSOS = 20;
    static final int MAX_IDIOMAS = 8;
    static final int MAX_BULLETS = 8;
    static final int MAX_TECNOLOGIAS = 25;
    static final int MAX_HABILIDADES = 50;

    private static final Pattern DATA = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");
    private static final Pattern ANO = Pattern.compile("\\d{4}");
    private static final Pattern ID_VALIDO = Pattern.compile("[a-z0-9-]{1,24}");
    private static final Set<String> SITUACOES = Set.of("CURSANDO", "CONCLUIDO", "TRANCADO");
    private static final String ALFABETO = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final Set<String> idsUsados = new HashSet<>();

    private NormalizadorPerfil() {
    }

    public static PerfilCurriculo normalizar(PerfilCurriculo perfil) {
        return new NormalizadorPerfil().aplicar(perfil == null ? PerfilCurriculo.vazio() : perfil);
    }

    private PerfilCurriculo aplicar(PerfilCurriculo p) {
        Contato c = p.contato() == null ? PerfilCurriculo.vazio().contato() : p.contato();
        Contato contato = new Contato(
                texto(c.nome(), 120, "O nome"),
                email(c.email()),
                texto(c.telefone(), 40, "O telefone"),
                texto(c.cidade(), 100, "A cidade"),
                LinkSeguro.validar(texto(c.linkedin(), 300, "O LinkedIn"), "O LinkedIn"),
                LinkSeguro.validar(texto(c.github(), 300, "O GitHub"), "O GitHub"),
                LinkSeguro.validar(texto(c.portfolio(), 300, "O portfólio"), "O portfólio"));

        List<Experiencia> experiencias = lista(p.experiencias(), MAX_EXPERIENCIAS, "experiências", (e) -> {
            String rotulo = "Experiência \"" + valorOu(e.cargo(), "sem cargo") + "\"";
            String inicio = data(e.inicio(), rotulo + ": início");
            String fim = data(e.fim(), rotulo + ": fim");
            ordem(inicio, fim, rotulo);
            return new Experiencia(id(e.id(), "e"),
                    obrigatorio(e.cargo(), 150, "Informe o cargo de todas as experiências"),
                    obrigatorio(e.empresa(), 150, rotulo + ": informe a empresa"),
                    texto(e.local(), 100, rotulo + ": o local"),
                    inicio, fim, bullets(e.bullets(), rotulo), termos(e.tecnologias(), MAX_TECNOLOGIAS, rotulo + ": tecnologias"));
        });
        List<Projeto> projetos = lista(p.projetos(), MAX_PROJETOS, "projetos", (pr) -> {
            String rotulo = "Projeto \"" + valorOu(pr.nome(), "sem nome") + "\"";
            return new Projeto(id(pr.id(), "p"),
                    obrigatorio(pr.nome(), 150, "Informe o nome de todos os projetos"),
                    LinkSeguro.validar(texto(pr.link(), 300, rotulo + ": o link"), rotulo + ": o link"),
                    bullets(pr.bullets(), rotulo), termos(pr.tecnologias(), MAX_TECNOLOGIAS, rotulo + ": tecnologias"));
        });
        List<Formacao> formacoes = lista(p.formacoes(), MAX_FORMACOES, "formações", (f) -> {
            String rotulo = "Formação \"" + valorOu(f.curso(), "sem curso") + "\"";
            String inicio = data(f.inicio(), rotulo + ": início");
            String fim = data(f.fim(), rotulo + ": conclusão");
            ordem(inicio, fim, rotulo);
            String situacao = f.situacao() == null || f.situacao().isBlank() ? null : f.situacao().strip().toUpperCase(Locale.ROOT);
            if (situacao != null && !SITUACOES.contains(situacao)) {
                throw invalido(rotulo + ": situação inválida");
            }
            return new Formacao(id(f.id(), "f"),
                    obrigatorio(f.curso(), 150, "Informe o curso de todas as formações"),
                    obrigatorio(f.instituicao(), 150, rotulo + ": informe a instituição"),
                    inicio, fim, situacao);
        });
        List<Curso> cursos = lista(p.cursos(), MAX_CURSOS, "cursos", (cu) -> {
            String ano = texto(cu.ano(), 4, "O ano do curso");
            if (ano != null && !ANO.matcher(ano).matches()) {
                throw invalido("Curso \"" + valorOu(cu.nome(), "sem nome") + "\": o ano deve ter 4 dígitos");
            }
            return new Curso(id(cu.id(), "c"), obrigatorio(cu.nome(), 150, "Informe o nome de todos os cursos"),
                    texto(cu.instituicao(), 150, "A instituição do curso"), ano);
        });
        List<Idioma> idiomas = lista(p.idiomas(), MAX_IDIOMAS, "idiomas", (i) -> new Idioma(id(i.id(), "i"),
                obrigatorio(i.idioma(), 40, "Informe o nome de todos os idiomas"),
                texto(i.nivel(), 40, "O nível do idioma")));

        return new PerfilCurriculo(contato, texto(p.resumo(), 1500, "O resumo"), experiencias, projetos,
                formacoes, cursos, termos(p.habilidades(), MAX_HABILIDADES, "Habilidades"), idiomas);
    }

    private List<Item> bullets(List<Item> bullets, String rotulo) {
        List<Item> limpos = new ArrayList<>();
        for (Item item : bullets == null ? List.<Item>of() : bullets) {
            String textoItem = texto(item == null ? null : item.texto(), 400, rotulo + ": cada tópico");
            if (textoItem != null) {
                limpos.add(new Item(id(item.id(), "b"), textoItem));
            }
        }
        if (limpos.size() > MAX_BULLETS) {
            throw invalido(rotulo + ": no máximo " + MAX_BULLETS + " tópicos");
        }
        return limpos;
    }

    /** Remove vazios e repetidos (sem diferenciar maiúsculas e acentos), mantendo a primeira grafia. */
    private static List<String> termos(List<String> valores, int maximo, String rotulo) {
        Map<String, String> unicos = new LinkedHashMap<>();
        for (String valor : valores == null ? List.<String>of() : valores) {
            String limpo = texto(valor, 50, rotulo + ": cada item");
            if (limpo != null) {
                unicos.putIfAbsent(Termos.normalizar(limpo), limpo);
            }
        }
        if (unicos.size() > maximo) {
            throw invalido(rotulo + ": no máximo " + maximo + " itens");
        }
        return List.copyOf(unicos.values());
    }

    private static <T, R> List<R> lista(List<T> itens, int maximo, String rotulo, Function<T, R> conversor) {
        List<T> entrada = itens == null ? List.of() : itens.stream().filter(java.util.Objects::nonNull).toList();
        if (entrada.size() > maximo) {
            throw invalido("No máximo " + maximo + " " + rotulo);
        }
        return entrada.stream().map(conversor).toList();
    }

    private String id(String atual, String prefixo) {
        if (atual != null && ID_VALIDO.matcher(atual).matches() && idsUsados.add(atual)) {
            return atual;
        }
        String novo;
        do {
            StringBuilder sufixo = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sufixo.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
            }
            novo = prefixo + "-" + sufixo;
        } while (!idsUsados.add(novo));
        return novo;
    }

    private static String data(String valor, String rotulo) {
        String limpo = texto(valor, 7, rotulo);
        if (limpo != null && !DATA.matcher(limpo).matches()) {
            throw invalido(rotulo + " deve estar no formato aaaa-mm");
        }
        return limpo;
    }

    private static void ordem(String inicio, String fim, String rotulo) {
        if (inicio != null && fim != null && fim.compareTo(inicio) < 0) {
            throw invalido(rotulo + ": o fim não pode ser antes do início");
        }
    }

    private static String email(String valor) {
        String limpo = texto(valor, 150, "O e-mail");
        if (limpo != null && !limpo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw invalido("O e-mail não parece válido");
        }
        return limpo;
    }

    private static String obrigatorio(String valor, int maximo, String mensagem) {
        String limpo = texto(valor, maximo, mensagem);
        if (limpo == null) {
            throw invalido(mensagem);
        }
        return limpo;
    }

    private static String texto(String valor, int maximo, String rotulo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpo = valor.strip();
        if (limpo.length() > maximo) {
            throw invalido(rotulo + " passa de " + maximo + " caracteres");
        }
        return limpo;
    }

    private static String valorOu(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor.strip();
    }

    private static ResponseStatusException invalido(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
