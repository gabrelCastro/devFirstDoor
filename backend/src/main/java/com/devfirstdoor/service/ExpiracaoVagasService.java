package com.devfirstdoor.service;

import com.devfirstdoor.repository.VagaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Remove as vagas que saíram das fontes: as que não reaparecem numa coleta há mais de
 * {@code app.crawler.dias-para-expirar} dias.
 *
 * É chamado por fonte e só depois de uma coleta bem-sucedida dela (ver ColetaService),
 * para que uma fonte fora do ar não faça todas as vagas dela sumirem.
 */
@Service
public class ExpiracaoVagasService {

    private final VagaRepository vagaRepository;
    private final int diasParaExpirar;

    public ExpiracaoVagasService(VagaRepository vagaRepository,
                                 @Value("${app.crawler.dias-para-expirar:7}") int diasParaExpirar) {
        this.vagaRepository = vagaRepository;
        this.diasParaExpirar = diasParaExpirar;
    }

    @Transactional
    public int removerExpiradas(String fonte, LocalDateTime agora) {
        return vagaRepository.deleteExpiradas(fonte, agora.minusDays(diasParaExpirar));
    }
}
