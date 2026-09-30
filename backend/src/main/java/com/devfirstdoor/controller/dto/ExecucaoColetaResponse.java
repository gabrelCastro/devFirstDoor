package com.devfirstdoor.controller.dto;

import com.devfirstdoor.domain.ExecucaoColeta;
import com.devfirstdoor.domain.ExecucaoFonte;
import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.domain.StatusExecucao;
import com.devfirstdoor.domain.StatusFonte;

import java.time.LocalDateTime;
import java.util.List;

public record ExecucaoColetaResponse(
        Long id,
        OrigemColeta origem,
        LocalDateTime inicio,
        LocalDateTime fim,
        StatusExecucao status,
        List<Fonte> fontes
) {

    public static ExecucaoColetaResponse from(ExecucaoColeta execucao, List<ExecucaoFonte> fontes) {
        return new ExecucaoColetaResponse(
                execucao.getId(),
                execucao.getOrigem(),
                execucao.getInicio(),
                execucao.getFim(),
                execucao.getStatus(),
                fontes.stream().map(Fonte::from).toList()
        );
    }

    public record Fonte(
            String fonte,
            LocalDateTime inicio,
            LocalDateTime fim,
            StatusFonte status,
            int encontradas,
            int novas,
            int expiradas,
            String mensagemErro
    ) {

        public static Fonte from(ExecucaoFonte resultado) {
            return new Fonte(
                    resultado.getFonte(),
                    resultado.getInicio(),
                    resultado.getFim(),
                    resultado.getStatus(),
                    resultado.getEncontradas(),
                    resultado.getNovas(),
                    resultado.getExpiradas(),
                    resultado.getMensagemErro()
            );
        }
    }
}
