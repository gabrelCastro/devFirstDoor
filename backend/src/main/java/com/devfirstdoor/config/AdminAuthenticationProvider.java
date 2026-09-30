package com.devfirstdoor.config;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Autentica o único usuário admin contra {@code app.admin.usuario}/{@code app.admin.senha}.
 * A comparação é em tempo constante ({@link MessageDigest#isEqual}) para não vazar, pelo tempo
 * de resposta, quantos caracteres da senha estão certos. Sem senha configurada, recusa tudo.
 */
public class AdminAuthenticationProvider implements AuthenticationProvider {

    private final AdminProperties properties;

    public AdminAuthenticationProvider(AdminProperties properties) {
        this.properties = properties;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String usuario = authentication.getName();
        Object credenciais = authentication.getCredentials();
        String senha = credenciais == null ? "" : credenciais.toString();

        boolean usuarioConfere = iguais(usuario, properties.getUsuario());
        boolean senhaConfere = iguais(senha, properties.getSenha());
        if (!properties.isHabilitado() || !usuarioConfere || !senhaConfere) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        return UsernamePasswordAuthenticationToken.authenticated(
                properties.getUsuario(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static boolean iguais(String informado, String esperado) {
        byte[] a = informado == null ? new byte[0] : informado.getBytes(StandardCharsets.UTF_8);
        byte[] b = esperado == null ? new byte[0] : esperado.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
