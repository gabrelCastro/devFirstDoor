package com.devfirstdoor.repository;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.util.TextNormalizer;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros da listagem de vagas (abas de seção e abrangência + busca livre),
 * aplicados no banco para que a paginação e as contagens considerem todas as vagas.
 */
public record VagaFiltro(Secao secao, Escopo escopo, String q) {

    public enum Secao { TODAS, REMOTO, ESTAGIO }

    public enum Escopo { TODAS, NACIONAL, GRINGA }

    public VagaFiltro {
        secao = secao == null ? Secao.TODAS : secao;
        escopo = escopo == null ? Escopo.TODAS : escopo;
    }

    public VagaFiltro comSecao(Secao outraSecao) {
        return new VagaFiltro(outraSecao, escopo, q);
    }

    public VagaFiltro comEscopo(Escopo outroEscopo) {
        return new VagaFiltro(secao, outroEscopo, q);
    }

    /**
     * A busca compara com o texto normalizado persistido na vaga (ver ClassificacaoVaga),
     * então normaliza o termo do mesmo jeito para ignorar maiúsculas e acentos.
     */
    public Specification<Vaga> toSpecification() {
        String termo = TextNormalizer.normalizar(q);
        return (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            switch (secao) {
                case REMOTO -> condicoes.add(cb.isTrue(root.get("remoto")));
                case ESTAGIO -> condicoes.add(cb.equal(root.get("nivel"), NivelVaga.ESTAGIO));
                case TODAS -> { }
            }
            switch (escopo) {
                case NACIONAL -> condicoes.add(cb.isFalse(root.get("internacional")));
                case GRINGA -> condicoes.add(cb.isTrue(root.get("internacional")));
                case TODAS -> { }
            }
            if (!termo.isEmpty()) {
                condicoes.add(cb.like(root.get("textoBusca"), "%" + escaparLike(termo) + "%", '\\'));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
    }

    private static String escaparLike(String termo) {
        return termo.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
