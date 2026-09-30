package com.devfirstdoor.service;

import com.devfirstdoor.controller.dto.TesteBoardRequest;
import com.devfirstdoor.controller.dto.TesteBoardResponse;
import com.devfirstdoor.controller.dto.TesteBoardResponse.ExemploAprovado;
import com.devfirstdoor.controller.dto.TesteBoardResponse.ExemploReprovado;
import com.devfirstdoor.controller.dto.TesteBoardResponse.MotivoReprovacao;
import com.devfirstdoor.crawler.ConsultaBoard;
import com.devfirstdoor.crawler.greenhouse.GreenhouseApiClient;
import com.devfirstdoor.crawler.greenhouse.GreenhouseCrawlerProperties;
import com.devfirstdoor.crawler.greenhouse.GreenhouseVagaClassifier;
import com.devfirstdoor.crawler.greenhouse.dto.GreenhouseJobDto;
import com.devfirstdoor.crawler.lever.LeverApiClient;
import com.devfirstdoor.crawler.lever.LeverCrawlerProperties;
import com.devfirstdoor.crawler.lever.LeverVagaClassifier;
import com.devfirstdoor.crawler.lever.dto.LeverPostingDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service
public class TesteBoardService {

    private static final int LIMITE_EXEMPLOS = 10;

    private final GreenhouseApiClient greenhouseApiClient;
    private final LeverApiClient leverApiClient;
    private final GreenhouseVagaClassifier greenhouseClassifier;
    private final LeverVagaClassifier leverClassifier;

    public TesteBoardService(GreenhouseApiClient greenhouseApiClient, LeverApiClient leverApiClient,
                             GreenhouseCrawlerProperties greenhouseProperties,
                             LeverCrawlerProperties leverProperties) {
        this.greenhouseApiClient = greenhouseApiClient;
        this.leverApiClient = leverApiClient;
        this.greenhouseClassifier = new GreenhouseVagaClassifier(greenhouseProperties);
        this.leverClassifier = new LeverVagaClassifier(leverProperties);
    }

    public TesteBoardResponse testar(TesteBoardRequest request) {
        if (request.ats() == null) {
            throw new IllegalArgumentException("O ATS é obrigatório");
        }
        String empresa = request.empresa().strip();
        return switch (request.ats()) {
            case GREENHOUSE -> testarGreenhouse(empresa);
            case LEVER -> testarLever(empresa);
        };
    }

    private TesteBoardResponse testarGreenhouse(String empresa) {
        return montarResposta(
                TesteBoardRequest.Ats.GREENHOUSE,
                empresa,
                greenhouseApiClient.buscarBoard(empresa),
                GreenhouseJobDto::title,
                GreenhouseJobDto::absoluteUrl,
                this::motivoReprovacao
        );
    }

    private TesteBoardResponse testarLever(String empresa) {
        return montarResposta(
                TesteBoardRequest.Ats.LEVER,
                empresa,
                leverApiClient.buscarBoard(empresa),
                LeverPostingDto::text,
                LeverPostingDto::hostedUrl,
                this::motivoReprovacao
        );
    }

    private MotivoReprovacao motivoReprovacao(GreenhouseJobDto job) {
        if (job.id() == null || job.title() == null || job.absoluteUrl() == null) {
            return MotivoReprovacao.DADOS_INCOMPLETOS;
        }
        if (greenhouseClassifier.classificarNivel(job).isEmpty()) {
            return MotivoReprovacao.NIVEL;
        }
        if (!greenhouseClassifier.isRelevanteParaTech(job)) {
            return MotivoReprovacao.FORA_DE_TECNOLOGIA;
        }
        if (!greenhouseClassifier.isJava(job)) {
            return MotivoReprovacao.NAO_JAVA;
        }
        if (!greenhouseClassifier.isRemota(job)) {
            return MotivoReprovacao.NAO_REMOTA;
        }
        return null;
    }

    private MotivoReprovacao motivoReprovacao(LeverPostingDto posting) {
        if (posting.id() == null || posting.text() == null || posting.hostedUrl() == null) {
            return MotivoReprovacao.DADOS_INCOMPLETOS;
        }
        if (leverClassifier.classificarNivel(posting).isEmpty()) {
            return MotivoReprovacao.NIVEL;
        }
        if (!leverClassifier.isRelevanteParaTech(posting)) {
            return MotivoReprovacao.FORA_DE_TECNOLOGIA;
        }
        if (!leverClassifier.isJava(posting)) {
            return MotivoReprovacao.NAO_JAVA;
        }
        if (!leverClassifier.isRemota(posting)) {
            return MotivoReprovacao.NAO_REMOTA;
        }
        return null;
    }

    private <T> TesteBoardResponse montarResposta(TesteBoardRequest.Ats ats, String empresa,
                                                   ConsultaBoard<T> consulta,
                                                   Function<T, String> titulo,
                                                   Function<T, String> link,
                                                   Function<T, MotivoReprovacao> classificar) {
        if (!consulta.existe()) {
            return new TesteBoardResponse(ats, empresa, false, 0, 0, List.of(), List.of());
        }

        int totalAprovadas = 0;
        List<ExemploAprovado> aprovadas = new ArrayList<>();
        List<ExemploReprovado> reprovadas = new ArrayList<>();
        for (T vaga : consulta.vagas()) {
            MotivoReprovacao motivo = classificar.apply(vaga);
            if (motivo == null) {
                totalAprovadas++;
                if (aprovadas.size() < LIMITE_EXEMPLOS) {
                    aprovadas.add(new ExemploAprovado(titulo.apply(vaga), link.apply(vaga)));
                }
            } else if (reprovadas.size() < LIMITE_EXEMPLOS) {
                reprovadas.add(new ExemploReprovado(titulo.apply(vaga), link.apply(vaga), motivo));
            }
        }
        return new TesteBoardResponse(ats, empresa, true, consulta.vagas().size(), totalAprovadas,
                List.copyOf(aprovadas), List.copyOf(reprovadas));
    }
}
