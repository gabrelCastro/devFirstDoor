package com.devfirstdoor.repository;

import com.devfirstdoor.domain.Vaga;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VagaRepository extends JpaRepository<Vaga, Long> {

    boolean existsByHashDeduplicacao(String hashDeduplicacao);

    boolean existsByLink(String link);

    Page<Vaga> findAllByOrderByDataColetaDesc(Pageable pageable);
}
