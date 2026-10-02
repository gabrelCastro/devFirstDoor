package com.devfirstdoor.repository;

import com.devfirstdoor.domain.Candidatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CandidaturaRepository extends JpaRepository<Candidatura, Long> {

    @Query("select c from Candidatura c left join fetch c.vaga "
            + "where c.usuario.id = :usuarioId order by c.atualizadaEm desc, c.id desc")
    List<Candidatura> listarDoUsuario(long usuarioId);

    /** Busca só entre as candidaturas do usuário: a de outra conta é tratada como inexistente. */
    @Query("select c from Candidatura c left join fetch c.vaga where c.id = :id and c.usuario.id = :usuarioId")
    Optional<Candidatura> buscarDoUsuario(long id, long usuarioId);

    Optional<Candidatura> findByUsuarioIdAndVagaId(long usuarioId, long vagaId);

    long countByUsuarioId(long usuarioId);
}
