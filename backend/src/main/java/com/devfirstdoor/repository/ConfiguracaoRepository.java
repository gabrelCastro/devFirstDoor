package com.devfirstdoor.repository;

import com.devfirstdoor.domain.Configuracao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoRepository extends JpaRepository<Configuracao, String> {
}
