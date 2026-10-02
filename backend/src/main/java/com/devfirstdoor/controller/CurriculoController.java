package com.devfirstdoor.controller;

import com.devfirstdoor.controller.dto.NovaVersaoCurriculoRequest;
import com.devfirstdoor.controller.dto.StatusCurriculoResponse;
import com.devfirstdoor.controller.dto.VersaoCurriculoResponse;
import com.devfirstdoor.controller.dto.VersaoCurriculoResumoResponse;
import com.devfirstdoor.curriculo.AdaptacaoService;
import com.devfirstdoor.curriculo.Escolhas;
import com.devfirstdoor.curriculo.PerfilCurriculo;
import com.devfirstdoor.curriculo.PerfilCurriculoService;
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
}
