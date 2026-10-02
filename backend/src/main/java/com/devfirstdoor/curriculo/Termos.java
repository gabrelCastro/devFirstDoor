package com.devfirstdoor.curriculo;

import com.devfirstdoor.util.TextNormalizer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dicionário de tecnologias com sinônimos ("Postgres" = "PostgreSQL", "Spring" ≠ "Spring Boot").
 * Serve para casar a vaga com o perfil e, na validação, para achar tecnologias citadas num texto
 * gerado pela IA e conferir se elas existem nas fontes.
 */
public final class Termos {

    /** forma canônica → variações (todas comparadas já normalizadas). */
    private static final Map<String, List<String>> DICIONARIO = Map.ofEntries(
            Map.entry("java", List.of("java")),
            Map.entry("spring boot", List.of("spring boot", "springboot")),
            Map.entry("spring", List.of("spring", "spring framework", "spring mvc", "spring data", "spring security")),
            Map.entry("hibernate", List.of("hibernate", "jpa")),
            Map.entry("kotlin", List.of("kotlin")),
            Map.entry("python", List.of("python")),
            Map.entry("javascript", List.of("javascript", "js")),
            Map.entry("typescript", List.of("typescript", "ts")),
            Map.entry("react", List.of("react", "reactjs", "react.js")),
            Map.entry("angular", List.of("angular")),
            Map.entry("vue", List.of("vue", "vuejs", "vue.js")),
            Map.entry("node.js", List.of("node", "nodejs", "node.js")),
            Map.entry("html", List.of("html", "html5")),
            Map.entry("css", List.of("css", "css3")),
            Map.entry("sql", List.of("sql")),
            Map.entry("postgresql", List.of("postgresql", "postgres")),
            Map.entry("mysql", List.of("mysql")),
            Map.entry("oracle", List.of("oracle")),
            Map.entry("sql server", List.of("sql server", "sqlserver")),
            Map.entry("mongodb", List.of("mongodb", "mongo")),
            Map.entry("redis", List.of("redis")),
            Map.entry("docker", List.of("docker")),
            Map.entry("kubernetes", List.of("kubernetes", "k8s")),
            Map.entry("aws", List.of("aws", "amazon web services")),
            Map.entry("azure", List.of("azure")),
            Map.entry("gcp", List.of("gcp", "google cloud")),
            Map.entry("git", List.of("git")),
            Map.entry("github", List.of("github")),
            Map.entry("gitlab", List.of("gitlab")),
            Map.entry("maven", List.of("maven")),
            Map.entry("gradle", List.of("gradle")),
            Map.entry("junit", List.of("junit")),
            Map.entry("mockito", List.of("mockito")),
            Map.entry("testes", List.of("testes unitarios", "teste unitario", "testes automatizados", "unit tests", "tdd")),
            Map.entry("api rest", List.of("api rest", "apis rest", "rest api", "rest apis", "restful", "rest")),
            Map.entry("microsservicos", List.of("microsservicos", "microservicos", "microservices", "microsservico")),
            Map.entry("kafka", List.of("kafka")),
            Map.entry("rabbitmq", List.of("rabbitmq")),
            Map.entry("linux", List.of("linux")),
            Map.entry("ci/cd", List.of("ci/cd", "ci cd", "integracao continua", "github actions", "jenkins")),
            Map.entry("scrum", List.of("scrum", "metodologias ageis", "agil", "kanban")),
            Map.entry("c#", List.of("c#", ".net", "dotnet")),
            Map.entry("go", List.of("golang")),
            Map.entry("php", List.of("php")),
            Map.entry("ingles", List.of("ingles", "english"))
    );

    private static final List<Map.Entry<String, Pattern>> PADROES = compilar();

    private Termos() {
    }

    public static String normalizar(String texto) {
        return TextNormalizer.normalizar(texto);
    }

    /** Forma canônica de um termo ("Postgres" → "postgresql"); fora do dicionário, o próprio termo normalizado. */
    public static String canonico(String termo) {
        String normalizado = normalizar(termo);
        for (Map.Entry<String, Pattern> padrao : PADROES) {
            Matcher m = padrao.getValue().matcher(normalizado);
            if (m.matches()) {
                return padrao.getKey();
            }
        }
        return normalizado;
    }

    /** Tecnologias do dicionário citadas no texto, já na forma canônica. */
    public static Set<String> encontrar(String texto) {
        String normalizado = normalizar(texto);
        Set<String> achados = new LinkedHashSet<>();
        for (Map.Entry<String, Pattern> padrao : PADROES) {
            if (padrao.getValue().matcher(normalizado).find()) {
                achados.add(padrao.getKey());
            }
        }
        // "spring boot" também casa "spring": não conta a forma genérica quando a específica apareceu.
        if (achados.contains("spring boot") && !normalizado.replace("spring boot", "").contains("spring")) {
            achados.remove("spring");
        }
        return achados;
    }

    /** O termo aparece no texto (pelo dicionário, ou literalmente quando não está nele)? */
    public static boolean contem(String texto, String termo) {
        String alvo = canonico(termo);
        if (DICIONARIO.containsKey(alvo)) {
            return encontrar(texto).contains(alvo);
        }
        return !alvo.isBlank() && palavraInteira(Pattern.quote(alvo)).matcher(normalizar(texto)).find();
    }

    private static List<Map.Entry<String, Pattern>> compilar() {
        List<Map.Entry<String, Pattern>> padroes = new ArrayList<>();
        DICIONARIO.forEach((canonico, variacoes) -> {
            String alternativas = String.join("|", variacoes.stream().map(Pattern::quote).toList());
            padroes.add(Map.entry(canonico, palavraInteira("(?:" + alternativas + ")")));
        });
        return padroes;
    }

    /** Borda que aceita símbolos no termo (c#, node.js, ci/cd): não pode ter letra/número colado. */
    private static Pattern palavraInteira(String regex) {
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + regex + "(?![\\p{L}\\p{N}])");
    }
}
