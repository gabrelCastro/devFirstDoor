package com.devfirstdoor.repository;

import com.devfirstdoor.domain.AnaliseVaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnaliseVagaRepository extends JpaRepository<AnaliseVaga, Long> {

    Optional<AnaliseVaga> findByHashDescricao(String hashDescricao);
}
