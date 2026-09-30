package com.devfirstdoor.repository;

import com.devfirstdoor.domain.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VagaRepository extends JpaRepository<Vaga, Long>, JpaSpecificationExecutor<Vaga> {

    boolean existsByHashDeduplicacao(String hashDeduplicacao);

    boolean existsByLink(String link);

    @Query("select distinct v.fonte from Vaga v order by v.fonte")
    List<String> findFontes();

    /** Vagas salvas antes das colunas derivadas existirem (ver Vaga#atualizarCamposDerivados). */
    List<Vaga> findByRemotoIsNullOrInternacionalIsNullOrTextoBuscaIsNull();
}
