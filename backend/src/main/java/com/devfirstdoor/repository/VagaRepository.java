package com.devfirstdoor.repository;

import com.devfirstdoor.domain.StatusVaga;
import com.devfirstdoor.domain.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface VagaRepository extends JpaRepository<Vaga, Long>, JpaSpecificationExecutor<Vaga> {

    boolean existsByHashDeduplicacao(String hashDeduplicacao);

    boolean existsByLink(String link);

    @Query("select distinct v.fonte from Vaga v where v.status = :ativa or v.status is null order by v.fonte")
    List<String> findFontesAtivas(@Param("ativa") StatusVaga ativa);

    /** Vagas salvas antes das colunas derivadas existirem (ver Vaga#atualizarCamposDerivados). */
    List<Vaga> findByRemotoIsNullOrInternacionalIsNullOrTextoBuscaIsNull();

    @Modifying
    @Query("update Vaga v set v.dataUltimaVisita = :quando where v.hashDeduplicacao in :hashes")
    int atualizarDataUltimaVisita(@Param("hashes") Collection<String> hashes, @Param("quando") LocalDateTime quando);

    @Modifying
    @Query("update Vaga v set v.status = :ativa where v.hashDeduplicacao in :hashes and v.status = :expirada")
    int reativarExpiradas(@Param("hashes") Collection<String> hashes,
                          @Param("ativa") StatusVaga ativa,
                          @Param("expirada") StatusVaga expirada);

    /** Vagas salvas antes de a coluna dataUltimaVisita existir contam a partir da dataColeta. */
    @Modifying
    @Query("update Vaga v set v.status = :expirada where v.fonte = :fonte "
            + "and (v.status = :ativa or v.status is null) "
            + "and coalesce(v.dataUltimaVisita, v.dataColeta) < :limite")
    int marcarExpiradas(@Param("fonte") String fonte,
                        @Param("limite") LocalDateTime limite,
                        @Param("ativa") StatusVaga ativa,
                        @Param("expirada") StatusVaga expirada);
}
