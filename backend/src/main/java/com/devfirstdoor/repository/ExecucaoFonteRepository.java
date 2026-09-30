package com.devfirstdoor.repository;

import com.devfirstdoor.domain.ExecucaoFonte;
import com.devfirstdoor.domain.StatusFonte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExecucaoFonteRepository extends JpaRepository<ExecucaoFonte, Long> {

    List<ExecucaoFonte> findByExecucaoIdInOrderByIdAsc(Collection<Long> execucaoIds);

    Optional<ExecucaoFonte> findFirstByFonteOrderByInicioDescIdDesc(String fonte);

    /** As 3 bastam para a regra de {@link com.devfirstdoor.domain.SaudeCrawler#ALERTA}. */
    List<ExecucaoFonte> findTop3ByFonteAndStatusOrderByInicioDescIdDesc(String fonte, StatusFonte status);
}
