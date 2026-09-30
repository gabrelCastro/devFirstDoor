package com.devfirstdoor.repository;

import com.devfirstdoor.domain.MotivoDescarte;
import com.devfirstdoor.domain.VagaDescartada;
import com.devfirstdoor.util.TextNormalizer;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record VagaDescartadaFiltro(String fonte, MotivoDescarte motivo, String busca) {

    public Specification<VagaDescartada> toSpecification() {
        String fonteNormalizada = fonte == null ? "" : fonte.trim().toUpperCase(Locale.ROOT);
        String termo = TextNormalizer.normalizar(busca);
        return (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (!fonteNormalizada.isEmpty()) {
                condicoes.add(cb.equal(cb.upper(root.get("fonte")), fonteNormalizada));
            }
            if (motivo != null) {
                condicoes.add(cb.equal(root.get("motivo"), motivo));
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
