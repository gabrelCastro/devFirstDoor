package com.devfirstdoor.controller;

import com.devfirstdoor.controller.dto.AtualizacaoCandidaturaRequest;
import com.devfirstdoor.controller.dto.CandidaturaDetalheResponse;
import com.devfirstdoor.controller.dto.CandidaturaResponse;
import com.devfirstdoor.controller.dto.EventoCandidaturaResponse;
import com.devfirstdoor.controller.dto.NotaRequest;
import com.devfirstdoor.controller.dto.NovaCandidaturaRequest;
import com.devfirstdoor.controller.dto.ProximoPassoRequest;
import com.devfirstdoor.controller.dto.ResumoCandidaturasResponse;
import com.devfirstdoor.service.CandidaturaService;
import com.devfirstdoor.service.CandidaturasCsv;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Quadro de candidaturas do usuário logado (qualquer papel). */
@RestController
@RequestMapping("/api/candidaturas")
public class CandidaturaController {

    private final CandidaturaService candidaturaService;

    public CandidaturaController(CandidaturaService candidaturaService) {
        this.candidaturaService = candidaturaService;
    }

    @GetMapping
    public List<CandidaturaResponse> listar(Authentication authentication) {
        return candidaturaService.listar(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CandidaturaDetalheResponse criar(Authentication authentication,
                                            @Valid @RequestBody NovaCandidaturaRequest request) {
        return candidaturaService.criar(authentication.getName(), request, LocalDateTime.now());
    }

    @GetMapping("/resumo")
    public ResumoCandidaturasResponse resumo(Authentication authentication) {
        return candidaturaService.resumir(authentication.getName(), LocalDateTime.now());
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(Authentication authentication) {
        String csv = CandidaturasCsv.gerar(candidaturaService.listarParaExportar(authentication.getName()));
        String nome = "candidaturas-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nome).build().toString())
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/{id}")
    public CandidaturaDetalheResponse detalhar(Authentication authentication, @PathVariable long id) {
        return candidaturaService.detalhar(authentication.getName(), id);
    }

    @PatchMapping("/{id}")
    public CandidaturaDetalheResponse atualizar(Authentication authentication, @PathVariable long id,
                                                @Valid @RequestBody AtualizacaoCandidaturaRequest request) {
        return candidaturaService.atualizar(authentication.getName(), id, request, LocalDateTime.now());
    }

    @PutMapping("/{id}/proximo-passo")
    public CandidaturaDetalheResponse proximoPasso(Authentication authentication, @PathVariable long id,
                                                   @Valid @RequestBody ProximoPassoRequest request) {
        return candidaturaService.definirProximoPasso(authentication.getName(), id, request.texto(), request.data(),
                LocalDateTime.now());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(Authentication authentication, @PathVariable long id) {
        candidaturaService.excluir(authentication.getName(), id);
    }

    @PostMapping("/{id}/notas")
    @ResponseStatus(HttpStatus.CREATED)
    public EventoCandidaturaResponse adicionarNota(Authentication authentication, @PathVariable long id,
                                                   @Valid @RequestBody NotaRequest request) {
        return candidaturaService.adicionarNota(authentication.getName(), id, request.texto(), LocalDateTime.now());
    }

    @DeleteMapping("/{id}/notas/{notaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarNota(Authentication authentication, @PathVariable long id, @PathVariable long notaId) {
        candidaturaService.apagarNota(authentication.getName(), id, notaId);
    }
}
