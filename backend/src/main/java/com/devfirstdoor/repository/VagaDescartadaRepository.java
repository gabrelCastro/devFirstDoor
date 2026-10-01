package com.devfirstdoor.repository;

import com.devfirstdoor.domain.MotivoDescarte;
import com.devfirstdoor.domain.VagaDescartada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VagaDescartadaRepository
        extends JpaRepository<VagaDescartada, Long>, JpaSpecificationExecutor<VagaDescartada> {

    Optional<VagaDescartada> findByLinkAndMotivo(String link, MotivoDescarte motivo);

    @Modifying
    long deleteByDataDescarteBefore(LocalDateTime limite);

    @Query("select d.motivo as motivo, count(d) as total from VagaDescartada d group by d.motivo")
    List<ContagemMotivo> contarPorMotivo();

    @Query("select d.motivo as motivo, count(d) as total from VagaDescartada d "
            + "where d.dataDescarte >= :inicio group by d.motivo")
    List<ContagemMotivo> contarPorMotivoDesde(@Param("inicio") LocalDateTime inicio);

    interface ContagemMotivo {
        MotivoDescarte getMotivo();

        long getTotal();
    }
}
