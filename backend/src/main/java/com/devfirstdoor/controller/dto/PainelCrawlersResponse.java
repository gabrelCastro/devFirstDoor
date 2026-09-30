package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.domain.SaudeCrawler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado dos crawlers para o painel admin.
 *
 * @param coletaEmAndamento  null quando nenhuma coleta está rodando
 * @param agendamentoPausado coleta agendada pausada pelo painel (a próxima coleta prevista é pulada)
 */
public record PainelCrawlersResponse(
        ColetaEmAndamento coletaEmAndamento,
        boolean agendamentoPausado,
        List<Crawler> crawlers
) {

    public record ColetaEmAndamento(
            OrigemColeta origem,
            LocalDateTime inicio,
            String fonteAtual,
            String progresso
    ) {
    }

    /**
     * @param progresso     só preenchido enquanto a fonte está rodando e o crawler informa andamento
     * @param proximaColeta null com o agendamento desligado ou a fonte desligada
     */
    public record Crawler(
            String fonte,
            boolean ligada,
            boolean rodando,
            String progresso,
            ExecucaoColetaResponse.Fonte ultimaExecucao,
            LocalDateTime ultimoSucesso,
            SaudeCrawler saude,
            LocalDateTime proximaColeta
    ) {
    }
}
