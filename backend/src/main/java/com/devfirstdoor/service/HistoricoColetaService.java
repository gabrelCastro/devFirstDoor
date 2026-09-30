package com.devfirstdoor.service;

import com.devfirstdoor.controller.dto.ExecucaoColetaResponse;
import com.devfirstdoor.domain.ExecucaoColeta;
import com.devfirstdoor.domain.ExecucaoFonte;
import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.domain.StatusExecucao;
import com.devfirstdoor.domain.StatusFonte;
import com.devfirstdoor.repository.ExecucaoColetaRepository;
import com.devfirstdoor.repository.ExecucaoFonteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Grava o histórico das coletas para o painel admin: a execução é criada logo no início
 * (assim uma coleta em andamento já aparece) e cada fonte é gravada assim que termina.
 */
@Service
public class HistoricoColetaService {

    private final ExecucaoColetaRepository execucaoRepository;
    private final ExecucaoFonteRepository fonteRepository;

    public HistoricoColetaService(ExecucaoColetaRepository execucaoRepository, ExecucaoFonteRepository fonteRepository) {
        this.execucaoRepository = execucaoRepository;
        this.fonteRepository = fonteRepository;
    }

    public ExecucaoColeta iniciar(OrigemColeta origem) {
        return execucaoRepository.save(new ExecucaoColeta(origem, LocalDateTime.now()));
    }

    public void registrar(ExecucaoFonte resultado) {
        fonteRepository.save(resultado);
    }

    /** SUCESSO se nenhuma fonte falhou, ERRO se todas falharam, PARCIAL no meio disso. */
    public void finalizar(ExecucaoColeta execucao, List<ExecucaoFonte> resultados) {
        long erros = resultados.stream().filter(r -> r.getStatus() == StatusFonte.ERRO).count();
        StatusExecucao status = erros == 0 ? StatusExecucao.SUCESSO
                : erros == resultados.size() ? StatusExecucao.ERRO
                : StatusExecucao.PARCIAL;
        execucao.finalizar(LocalDateTime.now(), status);
        execucaoRepository.save(execucao);
    }

    /**
     * Mais recentes primeiro. A ordenação vinda da requisição é ignorada; os resultados
     * por fonte da página são buscados numa consulta só.
     */
    @Transactional(readOnly = true)
    public Page<ExecucaoColetaResponse> listar(Pageable pageable) {
        Page<ExecucaoColeta> execucoes = execucaoRepository.findAllByOrderByInicioDescIdDesc(
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
        List<Long> ids = execucoes.map(ExecucaoColeta::getId).getContent();
        Map<Long, List<ExecucaoFonte>> fontesPorExecucao = ids.isEmpty() ? Map.of()
                : fonteRepository.findByExecucaoIdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(r -> r.getExecucao().getId()));
        return execucoes.map(e -> ExecucaoColetaResponse.from(e, fontesPorExecucao.getOrDefault(e.getId(), List.of())));
    }
}
