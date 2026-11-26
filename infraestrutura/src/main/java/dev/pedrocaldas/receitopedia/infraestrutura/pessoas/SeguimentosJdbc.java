package dev.pedrocaldas.receitopedia.infraestrutura.pessoas;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import dev.pedrocaldas.receitopedia.dominio.comunicacao.ConversaIndisponivel;
import dev.pedrocaldas.receitopedia.dominio.comunicacao.OperacaoProibida;

@Component
public class SeguimentosJdbc {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate consultas;

    SeguimentosJdbc(JdbcTemplate jdbc, NamedParameterJdbcTemplate consultas) {
        this.jdbc = jdbc;
        this.consultas = consultas;
    }

    public void travarUsuarios(Collection<UUID> usuarios) {
        var travados = consultas.queryForList("select id from usuario where id in (:ids) order by id for update",
                Map.of("ids", usuarios), UUID.class);
        if (travados.size() != usuarios.size()) {
            throw new ConversaIndisponivel();
        }
    }

    public boolean segue(UUID seguidor, UUID seguido) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from seguimento where seguidor_id = ? and seguido_id = ?)
                """, Boolean.class, seguidor, seguido));
    }

    public void exigirAmigos(UUID ator, Collection<UUID> convidados) {
        if (convidados.isEmpty()) {
            return;
        }
        Long amigos = consultas.queryForObject("""
                select count(*) from seguimento ida join seguimento volta
                  on volta.seguidor_id = ida.seguido_id and volta.seguido_id = ida.seguidor_id
                where ida.seguidor_id = :ator and ida.seguido_id in (:ids)
                """, Map.of("ator", ator, "ids", convidados), Long.class);
        if (amigos != convidados.size()) {
            throw new OperacaoProibida("Você só pode adicionar amigos ao grupo.");
        }
    }
}
