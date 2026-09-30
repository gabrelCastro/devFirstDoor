package com.devfirstdoor.repository;

import com.devfirstdoor.domain.ExecucaoFonte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ExecucaoFonteRepository extends JpaRepository<ExecucaoFonte, Long> {

    List<ExecucaoFonte> findByExecucaoIdInOrderByIdAsc(Collection<Long> execucaoIds);
}
