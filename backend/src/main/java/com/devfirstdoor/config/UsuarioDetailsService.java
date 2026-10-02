package com.devfirstdoor.config;

import com.devfirstdoor.repository.UsuarioRepository;
import com.devfirstdoor.service.UsuarioService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/** Carrega as contas do banco para o {@code DaoAuthenticationProvider} do Spring Security. */
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return usuarioRepository.findByLogin(UsuarioService.normalizarLogin(username))
                .map(usuario -> User.withUsername(usuario.getLogin())
                        .password(usuario.getSenhaHash())
                        .roles(usuario.getPapel().name())
                        .disabled(!usuario.isAtivo())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
