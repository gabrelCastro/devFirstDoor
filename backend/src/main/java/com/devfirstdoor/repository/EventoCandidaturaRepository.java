package com.devfirstdoor.repository;

import com.devfirstdoor.domain.EventoCandidatura;
import com.devfirstdoor.domain.TipoEventoCandidatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EventoCandidaturaRepository extends JpaRepository<EventoCandidatura, Long> {

    List<EventoCandidatura> findByCandidaturaIdOrderByDataAscIdAsc(long candidaturaId);

    long countByCandidaturaId(long candidaturaId);

    Optional<EventoCandidatura> findByIdAndCandidaturaIdAndTipo(long id, long candidaturaId, TipoEventoCandidatura tipo);

    /** Criações e mudanças de etapa de todas as candidaturas do usuário, para o resumo. */
    @Query("select e from EventoCandidatura e join fetch e.candidatura c where c.usuario.id = :usuarioId "
            + "and e.etapaNova is not null order by e.data asc, e.id asc")
    List<EventoCandidatura> etapasDoUsuario(long usuarioId);
}
