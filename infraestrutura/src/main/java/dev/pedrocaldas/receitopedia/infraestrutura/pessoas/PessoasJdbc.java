package dev.pedrocaldas.receitopedia.infraestrutura.pessoas;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat;
import dev.pedrocaldas.receitopedia.aplicacao.eventos.Evento;
import dev.pedrocaldas.receitopedia.aplicacao.eventos.RegistroEventos;
import dev.pedrocaldas.receitopedia.aplicacao.pessoas.PessoasRepositorio;
import dev.pedrocaldas.receitopedia.aplicacao.pessoas.PessoasServico;

@Repository
class PessoasJdbc implements PessoasRepositorio {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate consultas;
    private final SeguimentosJdbc seguimentos;
    private final RegistroEventos eventos;

    PessoasJdbc(JdbcTemplate jdbc, NamedParameterJdbcTemplate consultas, SeguimentosJdbc seguimentos,
            RegistroEventos eventos) {
        this.jdbc = jdbc;
        this.consultas = consultas;
        this.seguimentos = seguimentos;
        this.eventos = eventos;
    }

    @Override
    public boolean segue(UUID seguidor, UUID seguido) {
        return seguimentos.segue(seguidor, seguido);
    }

    @Override
    @Transactional
    public void alterarSeguimento(UUID seguidor, UUID seguido, boolean seguir) {
        seguimentos.travarUsuarios(List.of(seguidor, seguido));
        int alterados = seguir
                ? jdbc.update("insert into seguimento (seguidor_id, seguido_id) values (?, ?) on conflict do nothing",
                        seguidor, seguido)
                : jdbc.update("delete from seguimento where seguidor_id = ? and seguido_id = ?", seguidor, seguido);
        if (alterados == 0) {
            return;
        }
        eventos.registrar(Evento.novo(PessoasServico.SEGUIMENTO_ALTERADO, seguidor, seguido));
        if (!seguimentos.segue(seguido, seguidor)) {
            return;
        }
        // A ordem textual do UUID coincide com a do banco, incluindo o bit mais significativo.
        boolean emOrdem = seguidor.toString().compareTo(seguido.toString()) < 0;
        UUID primeiro = emOrdem ? seguidor : seguido;
        UUID segundo = emOrdem ? seguido : seguidor;
        if (seguir) {
            ativarDm(primeiro, segundo);
        } else {
            var ids = jdbc.queryForList("select id from chat_conversa where dm_primeiro = ? and dm_segundo = ?",
                    UUID.class, primeiro, segundo);
            for (UUID id : ids) {
                jdbc.update("update chat_conversa set dm_ativa = false where id = ?", id);
                eventos.registrar(Evento.novo(DadosChat.DM_DESATIVADA, id, null));
            }
        }
    }

    private void ativarDm(UUID primeiro, UUID segundo) {
        UUID novoId = UUID.randomUUID();
        int inserida = jdbc.update("""
                insert into chat_conversa (id, tipo, dm_primeiro, dm_segundo, dm_ativa)
                values (?, 'DM', ?, ?, true) on conflict do nothing
                """, novoId, primeiro, segundo);
        UUID id = novoId;
        if (inserida == 1) {
            jdbc.batchUpdate("insert into chat_membro (conversa_id, usuario_id) values (?, ?)",
                    List.of(new Object[]{id, primeiro}, new Object[]{id, segundo}));
        } else {
            id = jdbc.queryForObject("select id from chat_conversa where dm_primeiro = ? and dm_segundo = ?",
                    UUID.class, primeiro, segundo);
            jdbc.update("update chat_conversa set dm_ativa = true where id = ?", id);
        }
        eventos.registrar(Evento.novo(inserida == 1 ? DadosChat.DM_CRIADA : DadosChat.DM_REATIVADA, id, null));
    }

    @Override
    public List<Pessoa> buscar(UUID ator, Lista lista, String trecho, String cursor, int limite) {
        String seguida = """
                exists (select 1 from seguimento s where s.seguidor_id = :ator and s.seguido_id = u.id)
                """;
        String filtro = switch (lista) {
            case SEGUINDO -> seguida;
            case TODOS -> "not " + seguida;
            case AMIGOS -> seguida + """
                    and exists (select 1 from seguimento s where s.seguidor_id = u.id and s.seguido_id = :ator)
                    """;
        };
        return consultas.query("""
                select u.id, u.nome_usuario, u.nome_chave from usuario u
                where u.id <> :ator and u.nome_chave > :cursor and u.nome_chave like :trecho escape '!'
                  and (%s)
                order by u.nome_chave limit :limite
                """.formatted(filtro), Map.of("ator", ator, "cursor", cursor, "trecho", trecho, "limite", limite),
                (rs, n) -> new Pessoa(rs.getObject("id", UUID.class), rs.getString("nome_usuario"),
                        rs.getString("nome_chave")));
    }
}
