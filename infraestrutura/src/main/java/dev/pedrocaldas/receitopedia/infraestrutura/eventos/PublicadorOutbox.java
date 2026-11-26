package dev.pedrocaldas.receitopedia.infraestrutura.eventos;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import dev.pedrocaldas.receitopedia.aplicacao.eventos.Evento;

@Component
@ConditionalOnProperty(name = "receitopedia.eventos.ativo", havingValue = "true")
class PublicadorOutbox {
    private static final Logger LOG = LoggerFactory.getLogger(PublicadorOutbox.class);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacoes;
    private final PublicacaoRabbit publicacao;

    PublicadorOutbox(JdbcTemplate jdbc, TransactionTemplate transacoes, PublicacaoRabbit publicacao) {
        this.jdbc = jdbc;
        this.transacoes = transacoes;
        this.publicacao = publicacao;
    }

    @Scheduled(fixedDelay = 500)
    void publicar() {
        try {
            transacoes.executeWithoutResult(status -> {
                var eventos = jdbc.query("""
                        select id, tipo, agregado, referencia from evento_outbox
                        where publicado_em is null order by criado_em, id limit 25 for update skip locked
                        """, (rs, n) -> new Evento(rs.getObject("id", UUID.class), rs.getString("tipo"),
                        rs.getObject("agregado", UUID.class), rs.getObject("referencia", UUID.class)));
                for (var evento : eventos) {
                    try {
                        publicacao.confirmar(TopologiaEventos.EVENTOS, TopologiaEventos.CHAVE, publicacao.mensagem(evento));
                        jdbc.update("update evento_outbox set publicado_em = current_timestamp where id = ?", evento.id());
                    } catch (Exception e) {
                        throw new IllegalStateException("Evento " + evento.id() + " continua no outbox", e);
                    }
                }
            });
        } catch (RuntimeException erro) {
            LOG.warn("Publicação será tentada novamente: {}", erro.getMessage());
        }
    }
}
