package com.devfirstdoor.service;

import com.devfirstdoor.repository.VagaRepository;
import com.devfirstdoor.domain.StatusVaga;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Expira as vagas que saíram das fontes: as que não reaparecem numa coleta há mais de
 * {@code app.crawler.dias-para-expirar} dias.
 *
 * É chamado por fonte e só depois de uma coleta bem-sucedida dela (ver ColetaService),
 * para que uma fonte fora do ar não faça todas as vagas dela sumirem.
 */
@Service
public class ExpiracaoVagasService {

    private final VagaRepository vagaRepository;
    private final int diasParaExpirar;
    private final ConfiguracaoService configuracaoService;

    @Autowired
    public ExpiracaoVagasService(VagaRepository vagaRepository, ConfiguracaoService configuracaoService) {
        this.vagaRepository = vagaRepository;
        this.configuracaoService = configuracaoService;
        this.diasParaExpirar = 0;
    }

    public ExpiracaoVagasService(VagaRepository vagaRepository, int diasParaExpirar) {
        this.vagaRepository = vagaRepository;
        this.diasParaExpirar = diasParaExpirar;
        this.configuracaoService = null;
    }

    @Transactional
    public int removerExpiradas(String fonte, LocalDateTime agora) {
        int dias = configuracaoService != null ? configuracaoService.obter().diasParaExpirar() : diasParaExpirar;
        return vagaRepository.marcarExpiradas(fonte, agora.minusDays(dias),
                StatusVaga.ATIVA, StatusVaga.EXPIRADA);
    }
}
