package com.devfirstdoor.controller;

import com.devfirstdoor.service.CandidaturaDuplicadaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Devolve os erros de contas e candidaturas no formato {@code {"mensagem": ...}} que o frontend exibe. */
@RestControllerAdvice(assignableTypes = {ContaController.class, AdminUsuarioController.class, AuthController.class,
        CandidaturaController.class})
public class ApiErrosHandler {

    @ExceptionHandler(CandidaturaDuplicadaException.class)
    public ResponseEntity<Map<String, Object>> duplicada(CandidaturaDuplicadaException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("mensagem", e.getMessage(), "id", e.getCandidaturaId()));
    }

    /** JSON malformado ou valor fora do enum (etapa inexistente, data inválida). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> ilegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", "Dados inválidos"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException e) {
        String mensagem = e.getReason() == null ? "Requisição recusada" : e.getReason();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("mensagem", mensagem));
    }

    /** Mesma resposta para conta inexistente, senha errada e conta desativada: não revela qual foi. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> naoAutenticado(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensagem", "Usuário ou senha incorretos"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalido(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getDefaultMessage())
                .findFirst()
                .orElse("Dados inválidos");
        return ResponseEntity.badRequest().body(Map.of("mensagem", mensagem));
    }
}
