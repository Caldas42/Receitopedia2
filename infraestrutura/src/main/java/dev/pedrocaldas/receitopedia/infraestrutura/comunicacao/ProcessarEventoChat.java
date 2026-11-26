package dev.pedrocaldas.receitopedia.infraestrutura.comunicacao;

import static dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.ALTERADA;
import static dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.ENVIADA;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import dev.pedrocaldas.receitopedia.aplicacao.eventos.Evento;
import dev.pedrocaldas.receitopedia.aplicacao.eventos.ProcessadorEvento;

@Component
class ProcessarEventoChat implements ProcessadorEvento {
    private final JdbcTemplate jdbc;

    private final MiniaturasChat miniaturas;

    ProcessarEventoChat(JdbcTemplate jdbc, MiniaturasChat miniaturas) {
        this.jdbc = jdbc;
        this.miniaturas = miniaturas;
    }

    @Override
    public boolean atende(String tipo) {
        return ENVIADA.equals(tipo) || ALTERADA.equals(tipo);
    }

    @Override
    public void processar(Evento evento) {
        if (!ENVIADA.equals(evento.tipo())) {
            return;
        }
        var pendentes = jdbc.queryForList("""
                select id from chat_mensagem where conversa_id = ? and not processada
                  and sequencia <= (select sequencia from chat_mensagem where id = ? and conversa_id = ?)
                order by sequencia
                """, UUID.class, evento.agregado(), evento.referencia(), evento.agregado());
        for (UUID mensagem : pendentes) {
            var anexos = jdbc.queryForList("""
                    select a.id from chat_anexo a join chat_mensagem m on m.id = a.mensagem_id
                    where m.id = ? and not m.apagada and a.tipo in ('JPEG', 'PNG') order by a.id
                    """, UUID.class, mensagem);
            for (UUID anexo : miniaturas.preparar(anexos)) {
                jdbc.update("update chat_anexo set miniatura = true where id = ?", anexo);
            }
        }
        jdbc.queryForObject("select id from chat_conversa where id = ? for update", UUID.class, evento.agregado());
        for (UUID mensagem : pendentes) {
            jdbc.update("""
                    insert into chat_recibo (mensagem_id, usuario_id, entregue_em)
                    select m.id, p.usuario_id, current_timestamp
                    from chat_mensagem m join chat_membro p on p.conversa_id = m.conversa_id
                    where m.id = ? and p.ativo and p.usuario_id <> m.autor_id
                    on conflict do nothing
                    """, mensagem);
            jdbc.update("update chat_mensagem set processada = true where id = ?", mensagem);
        }
    }
}
