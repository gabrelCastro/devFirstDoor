package com.devfirstdoor.curriculo;

import com.devfirstdoor.curriculo.PerfilCurriculo.Curso;
import com.devfirstdoor.curriculo.PerfilCurriculo.Experiencia;
import com.devfirstdoor.curriculo.PerfilCurriculo.Formacao;
import com.devfirstdoor.curriculo.PerfilCurriculo.Idioma;
import com.devfirstdoor.curriculo.PerfilCurriculo.Item;
import com.devfirstdoor.curriculo.PerfilCurriculo.Projeto;
import com.devfirstdoor.curriculo.VagaAnalisada.Requisito;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cruza os requisitos da vaga com o perfil, sem IA, usando o dicionário de {@link Termos}.
 * A IA recebe isso pronto: em vez de deduzir onde está cada coisa, só usa as evidências apontadas.
 */
public final class Casamento {

    public enum Status { TEM, FRACO, NAO_TEM }

    /** {@code evidencias} são ids de bullets, experiências, projetos, cursos, formações ou idiomas. */
    public record Evidencia(String requisitoId, Status status, List<String> evidencias) {
    }

    private Casamento() {
    }

    public static Map<String, Evidencia> calcular(PerfilCurriculo perfil, VagaAnalisada vaga) {
        Map<String, Evidencia> resultado = new LinkedHashMap<>();
        for (Requisito requisito : vaga.requisitos()) {
            List<String> termos = requisito.termos().isEmpty() ? List.of(requisito.texto()) : requisito.termos();
            Set<String> fortes = new LinkedHashSet<>();
            boolean soNasHabilidades = false;
            for (String termo : termos) {
                for (Experiencia e : perfil.experiencias()) {
                    procurarEmBloco(termo, e.id(), e.bullets(), e.tecnologias(), fortes);
                }
                for (Projeto p : perfil.projetos()) {
                    procurarEmBloco(termo, p.id(), p.bullets(), p.tecnologias(), fortes);
                }
                for (Curso c : perfil.cursos()) {
                    if (Termos.contem(c.nome(), termo)) fortes.add(c.id());
                }
                for (Formacao f : perfil.formacoes()) {
                    if (Termos.contem(f.curso(), termo)) fortes.add(f.id());
                }
                for (Idioma i : perfil.idiomas()) {
                    if (Termos.contem(i.idioma(), termo)) fortes.add(i.id());
                }
                if (perfil.habilidades().stream().anyMatch(h -> Termos.canonico(h).equals(Termos.canonico(termo))
                        || Termos.contem(h, termo))) {
                    soNasHabilidades = true;
                }
            }
            Status status = !fortes.isEmpty() ? Status.TEM : soNasHabilidades ? Status.FRACO : Status.NAO_TEM;
            resultado.put(requisito.id(), new Evidencia(requisito.id(), status, List.copyOf(fortes)));
        }
        return resultado;
    }

    /** Bullet que cita o termo vira evidência; tecnologia listada no bloco aponta para o bloco. */
    private static void procurarEmBloco(String termo, String blocoId, List<Item> bullets, List<String> tecnologias,
                                        Set<String> evidencias) {
        for (Item b : bullets) {
            if (Termos.contem(b.texto(), termo)) evidencias.add(b.id());
        }
        if (tecnologias.stream().anyMatch(t -> Termos.canonico(t).equals(Termos.canonico(termo)) || Termos.contem(t, termo))) {
            evidencias.add(blocoId);
        }
    }

    /** Palavras-chave da vaga presentes num texto (usado na cobertura antes/depois). */
    public static List<String> palavrasPresentes(String texto, List<String> palavras) {
        List<String> presentes = new ArrayList<>();
        for (String palavra : palavras) {
            if (Termos.contem(texto, palavra)) presentes.add(palavra);
        }
        return presentes;
    }
}
