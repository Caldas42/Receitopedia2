package dev.pedrocaldas.receitopedia.infraestrutura.eventos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "receitopedia.eventos.worker", havingValue = "true")
class SaudeWorker {
    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;

    SaudeWorker(JdbcTemplate jdbc, RabbitTemplate rabbit) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
    }

    @Scheduled(fixedDelay = 5000)
    void atualizar() throws IOException {
        jdbc.queryForObject("select 1", Integer.class);
        Long ativos = rabbit.execute(canal -> canal.consumerCount(TopologiaEventos.TRABALHO));
        if (ativos != null && ativos > 0) {
            Files.writeString(Path.of("/tmp/receitopedia-worker"), Long.toString(System.currentTimeMillis()));
        }
    }
}
