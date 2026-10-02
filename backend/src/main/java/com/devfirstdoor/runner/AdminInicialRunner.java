package com.devfirstdoor.runner;

import com.devfirstdoor.config.AdminProperties;
import com.devfirstdoor.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Cria ou sincroniza o admin de ADMIN_USER/ADMIN_PASSWORD no banco assim que a aplicação sobe. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminInicialRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialRunner.class);

    private final UsuarioService usuarioService;
    private final AdminProperties adminProperties;

    public AdminInicialRunner(UsuarioService usuarioService, AdminProperties adminProperties) {
        this.usuarioService = usuarioService;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(String... args) {
        if (usuarioService.sincronizarAdminInicial()) {
            log.info("Admin '{}' sincronizado a partir da configuração", adminProperties.getUsuario());
        } else {
            log.info("ADMIN_PASSWORD vazio: nenhum admin criado a partir da configuração");
        }
    }
}
