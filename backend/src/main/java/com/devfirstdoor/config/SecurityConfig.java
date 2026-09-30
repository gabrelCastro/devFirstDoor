package com.devfirstdoor.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Só {@code /api/admin/**} exige login; o resto (API pública, frontend, console do H2) segue aberto.
 *
 * HTTP Basic stateless: o frontend manda o header {@code Authorization} em toda requisição, sem
 * sessão nem cookie. Como o navegador não envia a credencial sozinho, CSRF não se aplica.
 */
@Configuration
@EnableConfigurationProperties(AdminProperties.class)
public class SecurityConfig {

    @Bean
    public AdminAuthenticationProvider adminAuthenticationProvider(AdminProperties properties) {
        return new AdminAuthenticationProvider(properties);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AdminAuthenticationProvider provider) throws Exception {
        // 401 sem o header WWW-Authenticate: com ele o navegador abre o popup nativo de login
        // em cima da tela de login do painel.
        AuthenticationEntryPoint naoAutorizado = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(provider)
                .httpBasic(basic -> basic.authenticationEntryPoint(naoAutorizado))
                .exceptionHandling(e -> e.authenticationEntryPoint(naoAutorizado))
                // O console do H2 é montado com frames da mesma origem.
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll());
        return http.build();
    }
}
