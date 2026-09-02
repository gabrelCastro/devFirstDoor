package com.devfirstdoor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Libera CORS para toda a API. Em produção o frontend acessa via proxy do nginx
 * (mesma origem), mas isso mantém o `npm run dev` do Vite funcional apontando
 * direto para o backend, sem configuração extra.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST");
    }
}
