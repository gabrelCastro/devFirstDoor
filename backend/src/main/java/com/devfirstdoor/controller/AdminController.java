package com.devfirstdoor.controller;

import com.devfirstdoor.controller.dto.ExecucaoColetaResponse;
import com.devfirstdoor.controller.dto.ConfiguracaoAdminRequest;
import com.devfirstdoor.controller.dto.ConfiguracaoAdminResponse;
import com.devfirstdoor.controller.dto.PainelCrawlersResponse;
import com.devfirstdoor.controller.dto.TesteBoardRequest;
import com.devfirstdoor.controller.dto.TesteBoardResponse;
import com.devfirstdoor.domain.OrigemColeta;
import com.devfirstdoor.service.ColetaService;
import com.devfirstdoor.service.ConfiguracaoService;
import com.devfirstdoor.service.EstadoCrawlersService;
import com.devfirstdoor.service.HistoricoColetaService;
import com.devfirstdoor.service.PausaAgendamento;
import com.devfirstdoor.service.TesteBoardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ColetaService coletaService;
    private final HistoricoColetaService historicoColetaService;
    private final EstadoCrawlersService estadoCrawlersService;
    private final PausaAgendamento pausaAgendamento;
    private final ConfiguracaoService configuracaoService;
    private final TesteBoardService testeBoardService;

    public AdminController(ColetaService coletaService, HistoricoColetaService historicoColetaService,
                           EstadoCrawlersService estadoCrawlersService, PausaAgendamento pausaAgendamento,
                           ConfiguracaoService configuracaoService, TesteBoardService testeBoardService) {
        this.coletaService = coletaService;
        this.historicoColetaService = historicoColetaService;
        this.estadoCrawlersService = estadoCrawlersService;
        this.pausaAgendamento = pausaAgendamento;
        this.configuracaoService = configuracaoService;
        this.testeBoardService = testeBoardService;
    }

    /** Usado pelo frontend para validar o login. */
    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        return Map.of("usuario", authentication.getName());
    }

    /**
     * Dispara uma coleta de todas as fontes em segundo plano: responde 202 na hora e o
     * andamento aparece em GET /crawlers. 409 se já houver coleta rodando.
     */
    @PostMapping("/coletas")
    public ResponseEntity<Map<String, String>> coletarAgora() {
        return responder(coletaService.disparar(OrigemColeta.MANUAL), null);
    }

    /** Igual a {@link #coletarAgora()}, só para uma fonte. 404 se não existir, 409 se desligada. */
    @PostMapping("/coletas/{fonte}")
    public ResponseEntity<Map<String, String>> coletarFonte(@PathVariable String fonte) {
        return responder(coletaService.disparar(OrigemColeta.MANUAL, fonte), fonte);
    }

    @PostMapping("/agendamento/pausar")
    public Map<String, Boolean> pausarAgendamento() {
        pausaAgendamento.pausar();
        return Map.of("pausado", true);
    }

    @PostMapping("/agendamento/retomar")
    public Map<String, Boolean> retomarAgendamento() {
        pausaAgendamento.retomar();
        return Map.of("pausado", false);
    }

    private static ResponseEntity<Map<String, String>> responder(ColetaService.Disparo disparo, String fonte) {
        HttpStatus status = switch (disparo) {
            case INICIADA -> HttpStatus.ACCEPTED;
            case JA_EM_ANDAMENTO, FONTE_DESLIGADA -> HttpStatus.CONFLICT;
            case FONTE_INEXISTENTE -> HttpStatus.NOT_FOUND;
        };
        String mensagem = switch (disparo) {
            case INICIADA -> "Coleta iniciada";
            case JA_EM_ANDAMENTO -> "Já existe uma coleta em andamento";
            case FONTE_DESLIGADA -> "A fonte " + fonte + " está desligada";
            case FONTE_INEXISTENTE -> "Fonte desconhecida: " + fonte;
        };
        return ResponseEntity.status(status).body(Map.of("mensagem", mensagem));
    }

    /**
     * Estado de cada fonte (inclusive as desligadas) e da coleta em andamento. O painel
     * consulta isto por polling.
     */
    @GetMapping("/crawlers")
    public PainelCrawlersResponse crawlers() {
        return estadoCrawlersService.consultar();
    }

    /** Histórico das coletas, mais recentes primeiro, com o resultado de cada fonte. */
    @GetMapping("/execucoes")
    public Page<ExecucaoColetaResponse> execucoes(@PageableDefault(size = 20) Pageable pageable) {
        return historicoColetaService.listar(pageable);
    }

    @GetMapping("/configuracao")
    public ConfiguracaoAdminResponse consultarConfiguracao() {
        return configuracaoService.consultar();
    }

    @PutMapping("/configuracao")
    public ConfiguracaoAdminResponse salvarConfiguracao(@RequestBody ConfiguracaoAdminRequest request) {
        return configuracaoService.salvar(request);
    }

    /** Testa um board sem incluí-lo na configuração nem salvar as vagas encontradas. */
    @PostMapping("/boards/testar")
    public TesteBoardResponse testarBoard(@Valid @RequestBody TesteBoardRequest request) {
        return testeBoardService.testar(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> configuracaoInvalida(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", e.getMessage()));
    }
}
