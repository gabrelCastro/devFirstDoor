package com.devfirstdoor.config;

import com.devfirstdoor.repository.UsuarioRepository;
import com.devfirstdoor.service.SessaoService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Contas no banco ({@link UsuarioDetailsService}) com papéis USUARIO e ADMIN:
 * {@code /api/admin/**} exige ADMIN; conta e candidaturas exigem qualquer conta (exceto cadastro
 * e login); o resto (API pública, frontend, console do H2) segue aberto.
 *
 * O frontend entra por {@code POST /api/auth/login} e manda {@code Authorization: Bearer <token>}
 * ({@link TokenAuthenticationFilter}); HTTP Basic continua aceito para scripts. Nada de cookie:
 * como o navegador não envia o header sozinho, CSRF não se aplica.
 */
@Configuration
@EnableConfigurationProperties(AdminProperties.class)
public class SecurityConfig {

    /** BCrypt, com o prefixo {@code {bcrypt}} no hash para permitir trocar o algoritmo no futuro. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UsuarioDetailsService usuarioDetailsService(UsuarioRepository usuarioRepository) {
        return new UsuarioDetailsService(usuarioRepository);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracao) throws Exception {
        return configuracao.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessaoService sessaoService) throws Exception {
        // 401 sem o header WWW-Authenticate: com ele o navegador abre o popup nativo de login
        // em cima da tela de login do painel.
        AuthenticationEntryPoint naoAutorizado = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint(naoAutorizado))
                .addFilterBefore(new TokenAuthenticationFilter(sessaoService, naoAutorizado),
                        BasicAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint(naoAutorizado))
                // O console do H2 é montado com frames da mesma origem.
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/conta", "/api/auth/login").permitAll()
                        .requestMatchers("/api/conta", "/api/conta/**", "/api/auth/**",
                                "/api/candidaturas", "/api/candidaturas/**").authenticated()
                        .anyRequest().permitAll());
        return http.build();
    }
}
