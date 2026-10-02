package com.devfirstdoor.controller;

import com.devfirstdoor.controller.dto.NovaVersaoCurriculoRequest;
import com.devfirstdoor.controller.dto.StatusCurriculoResponse;
import com.devfirstdoor.controller.dto.VersaoCurriculoResponse;
import com.devfirstdoor.controller.dto.VersaoCurriculoResumoResponse;
import com.devfirstdoor.curriculo.AdaptacaoService;
import com.devfirstdoor.curriculo.CurriculoFinal;
import com.devfirstdoor.curriculo.Escolhas;
import com.devfirstdoor.curriculo.PerfilCurriculo;
import com.devfirstdoor.curriculo.PerfilCurriculoService;
import com.devfirstdoor.curriculo.render.DocxCurriculo;
import com.devfirstdoor.curriculo.render.PdfCurriculo;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** Currículo da conta logada: perfil-mestre e versões adaptadas por vaga com IA. */
@RestController
@RequestMapping("/api/curriculo")
public class CurriculoController {

    private final PerfilCurriculoService perfilService;
    private final AdaptacaoService adaptacaoService;

    public CurriculoController(PerfilCurriculoService perfilService, AdaptacaoService adaptacaoService) {
        this.perfilService = perfilService;
        this.adaptacaoService = adaptacaoService;
    }

    /** Se a IA está configurada e quanto da cota diária já foi usado. */
    @GetMapping("/status")
    public StatusCurriculoResponse status(Authentication authentication) {
        return adaptacaoService.status(authentication.getName(), LocalDateTime.now());
    }

    @GetMapping("/versoes")
    public List<VersaoCurriculoResumoResponse> versoes(Authentication authentication) {
        return adaptacaoService.listar(authentication.getName());
    }

    /** Adapta o perfil para a vaga colada. Leva alguns segundos (duas chamadas à IA). */
    @PostMapping("/versoes")
    @ResponseStatus(HttpStatus.CREATED)
    public VersaoCurriculoResponse adaptar(Authentication authentication, @RequestBody NovaVersaoCurriculoRequest request) {
        return adaptacaoService.criar(authentication.getName(), request, LocalDateTime.now());
    }

    @GetMapping("/versoes/{id}")
    public VersaoCurriculoResponse versao(Authentication authentication, @PathVariable long id) {
        return adaptacaoService.detalhar(authentication.getName(), id);
    }

    @PutMapping("/versoes/{id}/escolhas")
    public VersaoCurriculoResponse escolhas(Authentication authentication, @PathVariable long id,
                                            @RequestBody Escolhas escolhas) {
        return adaptacaoService.atualizarEscolhas(authentication.getName(), id, escolhas, LocalDateTime.now());
    }

    @DeleteMapping("/versoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(Authentication authentication, @PathVariable long id) {
        adaptacaoService.excluir(authentication.getName(), id);
    }

    @GetMapping("/perfil")
    public PerfilCurriculo perfil(Authentication authentication) {
        return perfilService.obter(authentication.getName());
    }

    /** Substitui o perfil inteiro; devolve a versão normalizada (com os ids gerados). */
    @PutMapping("/perfil")
    public PerfilCurriculo salvar(Authentication authentication, @RequestBody PerfilCurriculo perfil) {
        return perfilService.salvar(authentication.getName(), perfil, LocalDateTime.now());
    }

    /** Currículo do perfil, sem adaptação. */
    @GetMapping("/perfil/{formato:pdf|docx}")
    public ResponseEntity<byte[]> baixarPerfil(Authentication authentication, @PathVariable String formato) {
        PerfilCurriculo perfil = perfilService.obter(authentication.getName());
        return arquivo(CurriculoFinal.doPerfil(perfil), formato);
    }

    @GetMapping("/versoes/{id}/{formato:pdf|docx}")
    public ResponseEntity<byte[]> baixarVersao(Authentication authentication, @PathVariable long id,
                                               @PathVariable String formato) {
        return arquivo(adaptacaoService.curriculoFinal(authentication.getName(), id), formato);
    }

    private static final MediaType DOCX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static ResponseEntity<byte[]> arquivo(CurriculoFinal curriculo, String formato) {
        if (curriculo.contato() == null || curriculo.contato().nome() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe seu nome no perfil antes de baixar o currículo.");
        }
        boolean pdf = "pdf".equals(formato);
        byte[] conteudo = pdf ? PdfCurriculo.gerar(curriculo) : DocxCurriculo.gerar(curriculo);
        return ResponseEntity.ok()
                .contentType(pdf ? MediaType.APPLICATION_PDF : DOCX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(nomeArquivo(curriculo.contato().nome()) + "." + formato).build().toString())
                .body(conteudo);
    }

    /** "Maria da Silva" → "Maria-da-Silva-Curriculo", como os recrutadores recomendam. */
    static String nomeArquivo(String nome) {
        String semAcento = java.text.Normalizer.normalize(nome, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String base = semAcento.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return (base.isEmpty() ? "" : base + "-") + "Curriculo";
    }
}
