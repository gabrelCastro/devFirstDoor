package com.devfirstdoor.service;

import com.devfirstdoor.domain.Candidatura;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exporta as candidaturas em CSV com ponto e vírgula e BOM, como o Excel em português espera.
 * Campos que começam com {@code = + - @} ganham um apóstrofo para o Excel não executá-los
 * como fórmula (o título e a empresa vêm de sites de terceiros ou do próprio usuário).
 */
public final class CandidaturasCsv {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String CABECALHO = String.join(";", "Empresa", "Vaga", "Local", "Link", "Fonte", "Etapa",
            "Na etapa desde", "Data da candidatura", "Próximo passo", "Data do próximo passo", "Criada em");

    private CandidaturasCsv() {
    }

    public static String gerar(List<Candidatura> candidaturas) {
        StringBuilder csv = new StringBuilder("﻿").append(CABECALHO).append("\r\n");
        for (Candidatura c : candidaturas) {
            csv.append(String.join(";",
                    campo(c.getEmpresa()),
                    campo(c.getTitulo()),
                    campo(c.getLocal()),
                    campo(c.getLink()),
                    campo(c.isExterna() ? "Externa" : c.getFonte()),
                    campo(c.getEtapa().rotulo()),
                    campo(dataHora(c.getEtapaDesde())),
                    campo(data(c.getDataCandidatura())),
                    campo(c.getProximoPasso()),
                    campo(data(c.getDataProximoPasso())),
                    campo(dataHora(c.getCriadaEm()))
            )).append("\r\n");
        }
        return csv.toString();
    }

    static String campo(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        String seguro = "=+-@\t\r".indexOf(valor.charAt(0)) >= 0 ? "'" + valor : valor;
        return '"' + seguro.replace("\"", "\"\"") + '"';
    }

    private static String data(LocalDate data) {
        return data == null ? null : DATA.format(data);
    }

    private static String dataHora(LocalDateTime data) {
        return data == null ? null : DATA_HORA.format(data);
    }
}
