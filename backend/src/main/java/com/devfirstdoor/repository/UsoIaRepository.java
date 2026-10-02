package com.devfirstdoor.repository;

import com.devfirstdoor.domain.UsoIa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface UsoIaRepository extends JpaRepository<UsoIa, Long> {

    @Query("select count(u) from UsoIa u where u.usuario.id = :usuarioId and u.criadoEm > :desde")
    long contarDesde(long usuarioId, LocalDateTime desde);
}
