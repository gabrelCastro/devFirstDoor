package com.devfirstdoor.repository;

import com.devfirstdoor.domain.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    @Query("select s from Sessao s join fetch s.usuario where s.tokenHash = :tokenHash")
    Optional<Sessao> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from Sessao s where s.expiraEm <= :agora")
    int apagarExpiradas(LocalDateTime agora);

    /** Revoga as sessões do usuário, menos a indicada (que pode ser nula: revoga todas). */
    @Modifying
    @Query("delete from Sessao s where s.usuario.id = :usuarioId and (:manterId is null or s.id <> :manterId)")
    int revogarOutras(Long usuarioId, Long manterId);
}
