package com.devfirstdoor.service;

import com.devfirstdoor.controller.dto.ExecucaoColetaResponse;
import com.devfirstdoor.controller.dto.PainelCrawlersResponse;
import com.devfirstdoor.controller.dto.PainelCrawlersResponse.ColetaEmAndamento;
import com.devfirstdoor.crawler.VagaCrawler;
import com.devfirstdoor.crawler.greenhouse.GreenhouseJobMapper;
import com.devfirstdoor.crawler.gupy.GupyJobMapper;
import com.devfirstdoor.crawler.lever.LeverJobMapper;
import com.devfirstdoor.crawler.linkedin.LinkedinJobMapper;
import com.devfirstdoor.crawler.programathor.ProgramathorJobMapper;
import com.devfirstdoor.crawler.remoteok.RemoteOkJobMapper;
import com.devfirstdoor.domain.ExecucaoFonte;
import com.devfirstdoor.domain.SaudeCrawler;
import com.devfirstdoor.domain.StatusFonte;
import com.devfirstdoor.repository.ExecucaoFonteRepository;
import com.devfirstdoor.runner.ColetaAgendadaRunner;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta o painel dos crawlers: junta o estado ao vivo ({@link AndamentoColeta}), o
 * histórico gravado e o agendamento.
 *
 * Lista todas as fontes conhecidas, e não só os beans registrados: o LinkedIn desligado
 * nem vira bean, mas precisa aparecer no painel como desligado.
 */
@Service
public class EstadoCrawlersService {

    static final List<String> FONTES_CONHECIDAS = List.of(
            GupyJobMapper.FONTE,
            ProgramathorJobMapper.FONTE,
            RemoteOkJobMapper.FONTE,
            LinkedinJobMapper.FONTE,
            GreenhouseJobMapper.FONTE,
            LeverJobMapper.FONTE
    );

    private final Map<String, VagaCrawler> crawlersPorFonte;
    private final ExecucaoFonteRepository fonteRepository;
    private final AndamentoColeta andamento;
    private final ObjectProvider<ColetaAgendadaRunner> agendamento;
    private final PausaAgendamento pausa;

    public EstadoCrawlersService(List<VagaCrawler> crawlers, ExecucaoFonteRepository fonteRepository,
                                 AndamentoColeta andamento, ObjectProvider<ColetaAgendadaRunner> agendamento,
                                 PausaAgendamento pausa) {
        this.crawlersPorFonte = crawlers.stream()
                .collect(Collectors.toMap(VagaCrawler::getFonte, Function.identity()));
        this.fonteRepository = fonteRepository;
        this.andamento = andamento;
        this.agendamento = agendamento;
        this.pausa = pausa;
    }

    @Transactional(readOnly = true)
    public PainelCrawlersResponse consultar() {
        Optional<AndamentoColeta.Estado> emAndamento = andamento.atual();
        ColetaAgendadaRunner runner = agendamento.getIfAvailable();
        LocalDateTime proximaColeta = runner != null ? runner.getProximaColeta() : null;

        List<PainelCrawlersResponse.Crawler> crawlers = fontes().stream()
                .map(fonte -> montar(fonte, emAndamento, proximaColeta))
                .toList();
        ColetaEmAndamento coleta = emAndamento
                .map(e -> new ColetaEmAndamento(e.origem(), e.inicio(), e.fonteAtual(), e.progresso()))
                .orElse(null);
        return new PainelCrawlersResponse(coleta, pausa.isPausado(), crawlers);
    }

    /** As conhecidas, na ordem fixa, mais qualquer crawler novo que ainda não esteja na lista. */
    private List<String> fontes() {
        List<String> fontes = new ArrayList<>(FONTES_CONHECIDAS);
        crawlersPorFonte.keySet().stream().filter(f -> !fontes.contains(f)).sorted().forEach(fontes::add);
        return fontes;
    }

    private PainelCrawlersResponse.Crawler montar(String fonte, Optional<AndamentoColeta.Estado> emAndamento,
                                                  LocalDateTime proximaColeta) {
        VagaCrawler crawler = crawlersPorFonte.get(fonte);
        boolean ligada = crawler != null && crawler.isLigada();
        boolean rodando = emAndamento.map(e -> fonte.equals(e.fonteAtual())).orElse(false);

        Optional<ExecucaoFonte> ultimaExecucao = fonteRepository.findFirstByFonteOrderByInicioDescIdDesc(fonte);
        List<ExecucaoFonte> ultimosSucessos =
                fonteRepository.findTop3ByFonteAndStatusOrderByInicioDescIdDesc(fonte, StatusFonte.SUCESSO);

        return new PainelCrawlersResponse.Crawler(
                fonte,
                ligada,
                rodando,
                rodando ? emAndamento.get().progresso() : null,
                ultimaExecucao.map(ExecucaoColetaResponse.Fonte::from).orElse(null),
                ultimosSucessos.isEmpty() ? null : ultimosSucessos.get(0).getFim(),
                SaudeCrawler.calcular(ligada, ultimaExecucao, ultimosSucessos),
                ligada ? proximaColeta : null
        );
    }
}
