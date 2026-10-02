package com.devfirstdoor.config;

import com.devfirstdoor.repository.UsuarioRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Contas no banco ({@link UsuarioDetailsService}) com papéis USUARIO e ADMIN:
 * {@code /api/admin/**} exige ADMIN, {@code /api/conta} exige qualquer conta (exceto o cadastro)
 * e o resto (API pública, frontend, console do H2) segue aberto.
 *
 * HTTP Basic stateless: o frontend manda o header {@code Authorization} em toda requisição, sem
 * sessão nem cookie. Como o navegador não envia a credencial sozinho, CSRF não se aplica.
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 401 sem o header WWW-Authenticate: com ele o navegador abre o popup nativo de login
        // em cima da tela de login do painel.
        AuthenticationEntryPoint naoAutorizado = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint(naoAutorizado))
                .exceptionHandling(e -> e.authenticationEntryPoint(naoAutorizado))
                // O console do H2 é montado com frames da mesma origem.
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/conta").permitAll()
                        .requestMatchers("/api/conta", "/api/conta/**").authenticated()
                        .anyRequest().permitAll());
        return http.build();
    }
}
