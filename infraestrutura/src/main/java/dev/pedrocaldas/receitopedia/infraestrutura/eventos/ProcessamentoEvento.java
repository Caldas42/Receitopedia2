package dev.pedrocaldas.receitopedia.infraestrutura.eventos;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.pedrocaldas.receitopedia.aplicacao.eventos.Evento;
import dev.pedrocaldas.receitopedia.aplicacao.eventos.ProcessadorEvento;

@Service
class ProcessamentoEvento {
    private final JdbcTemplate jdbc;
    private final List<ProcessadorEvento> processadores;

    ProcessamentoEvento(JdbcTemplate jdbc, List<ProcessadorEvento> processadores) {
        this.jdbc = jdbc;
        this.processadores = processadores;
    }

    @Transactional
    public void processar(Evento evento) {
        int novo = jdbc.update("insert into evento_processado (id) values (?) on conflict do nothing", evento.id());
        if (novo != 0) {
            processadores.stream().filter(processador -> processador.atende(evento.tipo())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Tipo de evento desconhecido"))
                    .processar(evento);
        }
    }
}
