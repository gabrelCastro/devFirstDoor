package com.devfirstdoor.repository;

import com.devfirstdoor.domain.VersaoCurriculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VersaoCurriculoRepository extends JpaRepository<VersaoCurriculo, Long> {

    @Query("select v from VersaoCurriculo v left join fetch v.candidatura where v.usuario.id = :usuarioId "
            + "order by v.criadaEm desc, v.id desc")
    List<VersaoCurriculo> listarDoUsuario(long usuarioId);

    @Query("select v from VersaoCurriculo v left join fetch v.candidatura where v.id = :id and v.usuario.id = :usuarioId")
    Optional<VersaoCurriculo> buscarDoUsuario(long id, long usuarioId);

    long countByUsuarioId(long usuarioId);
}
