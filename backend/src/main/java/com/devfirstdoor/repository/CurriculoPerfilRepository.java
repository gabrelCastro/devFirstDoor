package com.devfirstdoor.repository;

import com.devfirstdoor.domain.CurriculoPerfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurriculoPerfilRepository extends JpaRepository<CurriculoPerfil, Long> {

    Optional<CurriculoPerfil> findByUsuarioId(long usuarioId);
}
