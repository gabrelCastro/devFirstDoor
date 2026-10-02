package com.devfirstdoor.config;

import com.devfirstdoor.domain.Sessao;
import com.devfirstdoor.service.SessaoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Autentica {@code Authorization: Bearer <token>} pelas sessões do banco. Papel e ativação vêm
 * da conta a cada requisição, então rebaixar ou desativar alguém vale na hora. Token inválido ou
 * expirado responde 401 direto, em vez de seguir como anônimo.
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final SessaoService sessaoService;
    private final AuthenticationEntryPoint entryPoint;

    public TokenAuthenticationFilter(SessaoService sessaoService, AuthenticationEntryPoint entryPoint) {
        this.sessaoService = sessaoService;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho == null || !cabecalho.regionMatches(true, 0, PREFIXO, 0, PREFIXO.length())) {
            chain.doFilter(request, response);
            return;
        }
        String token = cabecalho.substring(PREFIXO.length()).trim();
        Sessao sessao = token.isEmpty() ? null : sessaoService.validar(token, LocalDateTime.now()).orElse(null);
        if (sessao == null) {
            SecurityContextHolder.clearContext();
            entryPoint.commence(request, response, new BadCredentialsException("Sessão inválida ou expirada"));
            return;
        }
        var usuario = sessao.getUsuario();
        var autenticacao = UsernamePasswordAuthenticationToken.authenticated(usuario.getLogin(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPapel().name())));
        autenticacao.setDetails(new SessaoAutenticada(sessao.getId()));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        chain.doFilter(request, response);
    }
}
