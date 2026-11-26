package dev.pedrocaldas.receitopedia.apresentacao.usuarios;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.pedrocaldas.receitopedia.aplicacao.usuarios.PromoverAdmin;
import dev.pedrocaldas.receitopedia.aplicacao.usuarios.UsuarioRepositorio;

@Configuration
class AdminInicial {
    private static final Logger LOG = LoggerFactory.getLogger(AdminInicial.class);

    @Bean
    PromoverAdmin promoverAdmin(UsuarioRepositorio repositorio) {
        return new PromoverAdmin(repositorio);
    }

    @Bean
    ApplicationRunner promoverAdminConfigurado(PromoverAdmin promover,
            @Value("${RECEITOPEDIA_ADMIN_EMAIL:}") String email) {
        return argumentos -> {
            if (!email.isBlank() && !promover.executar(email)) {
                LOG.warn("A conta configurada para administração ainda não foi cadastrada.");
            }
        };
    }
}
