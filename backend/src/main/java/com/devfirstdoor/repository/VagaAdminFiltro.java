package com.devfirstdoor.repository;

import com.devfirstdoor.domain.StatusVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.util.TextNormalizer;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Filtros próprios da moderação, que também enxerga vagas expiradas e ocultas. */
public record VagaAdminFiltro(StatusVaga status, String fonte, String busca) {

    public Specification<Vaga> toSpecification() {
        String termo = TextNormalizer.normalizar(busca);
        String fonteNormalizada = fonte == null ? "" : fonte.trim().toUpperCase(Locale.ROOT);
        return (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (status == StatusVaga.ATIVA) {
                condicoes.add(cb.or(cb.equal(root.get("status"), StatusVaga.ATIVA), cb.isNull(root.get("status"))));
            } else if (status != null) {
                condicoes.add(cb.equal(root.get("status"), status));
            }
            if (!fonteNormalizada.isEmpty()) {
                condicoes.add(cb.equal(cb.upper(root.get("fonte")), fonteNormalizada));
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
