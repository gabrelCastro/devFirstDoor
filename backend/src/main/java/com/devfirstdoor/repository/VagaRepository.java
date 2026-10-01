package com.devfirstdoor.repository;

import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.StatusVaga;
import com.devfirstdoor.domain.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface VagaRepository extends JpaRepository<Vaga, Long>, JpaSpecificationExecutor<Vaga> {

    boolean existsByHashDeduplicacao(String hashDeduplicacao);

    boolean existsByLink(String link);

    @Query("select distinct v.fonte from Vaga v where v.status = :ativa or v.status is null order by v.fonte")
    List<String> findFontesAtivas(@Param("ativa") StatusVaga ativa);

    /** Vagas salvas antes das colunas derivadas existirem (ver Vaga#atualizarCamposDerivados). */
    @Query("select v from Vaga v where v.remoto is null or v.internacional is null "
            + "or v.textoBusca is null or v.tituloNormalizado is null or v.empresaNormalizada is null")
    List<Vaga> findComCamposDerivadosPendentes();

    @Query("select v.fonte as chave, count(v) as total from Vaga v "
            + "where v.status = :ativa or v.status is null group by v.fonte order by v.fonte")
    List<ContagemTexto> contarAtivasPorFonte(@Param("ativa") StatusVaga ativa);

    @Query("select v.nivel as nivel, count(v) as total from Vaga v "
            + "where v.status = :ativa or v.status is null group by v.nivel")
    List<ContagemNivel> contarAtivasPorNivel(@Param("ativa") StatusVaga ativa);

    @Query("select coalesce(v.remoto, false) as remoto, count(v) as total from Vaga v "
            + "where v.status = :ativa or v.status is null group by coalesce(v.remoto, false)")
    List<ContagemModalidade> contarAtivasPorModalidade(@Param("ativa") StatusVaga ativa);

    @Query("select cast(v.dataColeta as LocalDate) as data, v.fonte as fonte, count(v) as total "
            + "from Vaga v where v.dataColeta >= :inicio "
            + "group by cast(v.dataColeta as LocalDate), v.fonte "
            + "order by cast(v.dataColeta as LocalDate), v.fonte")
    List<ContagemNovasDia> contarNovasPorDiaEFonteDesde(@Param("inicio") LocalDateTime inicio);

    @Query("select avg(timestampdiff(SECOND, v.dataColeta, coalesce(v.dataUltimaVisita, v.dataColeta))) "
            + "from Vaga v where v.status = :expirada")
    Double calcularTempoMedioNoArEmSegundos(@Param("expirada") StatusVaga expirada);

    @Query("select v.fonte as chave, count(v) as total from Vaga v "
            + "where (v.status = :ativa or v.status is null) "
            + "and v.tituloNormalizado is not null and v.empresaNormalizada is not null "
            + "and not exists (select outra.id from Vaga outra "
            + "where (outra.status = :ativa or outra.status is null) and outra.fonte <> v.fonte "
            + "and outra.tituloNormalizado = v.tituloNormalizado "
            + "and outra.empresaNormalizada = v.empresaNormalizada) "
            + "group by v.fonte order by v.fonte")
    List<ContagemTexto> contarAtivasExclusivasPorFonte(@Param("ativa") StatusVaga ativa);

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

    interface ContagemTexto {
        String getChave();

        long getTotal();
    }

    interface ContagemNivel {
        NivelVaga getNivel();

        long getTotal();
    }

    interface ContagemModalidade {
        boolean getRemoto();

        long getTotal();
    }

    interface ContagemNovasDia {
        LocalDate getData();

        String getFonte();

        long getTotal();
    }
}
