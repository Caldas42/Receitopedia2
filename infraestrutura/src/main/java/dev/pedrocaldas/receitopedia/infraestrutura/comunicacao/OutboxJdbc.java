package dev.pedrocaldas.receitopedia.infraestrutura.comunicacao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import dev.pedrocaldas.receitopedia.aplicacao.eventos.Evento;
import dev.pedrocaldas.receitopedia.aplicacao.eventos.RegistroEventos;

@Repository
class OutboxJdbc implements RegistroEventos {
    private final JdbcTemplate jdbc;

    OutboxJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Evento evento) {
        jdbc.update("insert into evento_outbox (id, tipo, agregado, referencia) values (?, ?, ?, ?)",
                evento.id(), evento.tipo(), evento.agregado(), evento.referencia());
    }
}
