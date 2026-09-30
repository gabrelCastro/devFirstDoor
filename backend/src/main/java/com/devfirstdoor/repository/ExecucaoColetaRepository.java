package com.devfirstdoor.repository;

import com.devfirstdoor.domain.ExecucaoColeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecucaoColetaRepository extends JpaRepository<ExecucaoColeta, Long> {

    Page<ExecucaoColeta> findAllByOrderByInicioDescIdDesc(Pageable pageable);
}
