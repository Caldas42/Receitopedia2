package dev.pedrocaldas.receitopedia.apresentacao.pessoas;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class MigracaoLegadoTest {
    @Test
    void conservaMensagemLegadaEPreencheTituloAoMigrarDaVersaoOnze() throws Exception {
        String banco = "jdbc:h2:mem:legado_pessoas;MODE=PostgreSQL";
        try (var conexao = DriverManager.getConnection(banco, "sa", "");
                var sql = conexao.createStatement()) {
            Flyway.configure().dataSource(banco, "sa", "").target("11").load().migrate();
            sql.executeUpdate("""
                    insert into usuario (id,email,nome_usuario,nome_chave,senha_hash,papel)
                    values ('f0000000-0000-0000-0000-000000000001','ana@example.com','Ana','ana','teste','USER'),
                           ('10000000-0000-0000-0000-000000000001','bia@example.com','Bia','bia','teste','USER')
                    """);
            sql.executeUpdate("""
                    insert into chat_conversa (id,tipo,dm_primeiro,dm_segundo,ultima_seq)
                    values ('30000000-0000-0000-0000-000000000001','DM',
                            'f0000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',1)
                    """);
            sql.executeUpdate("""
                    insert into chat_mensagem (id,conversa_id,autor_id,sequencia,texto,texto_chave,enviada_em)
                    values ('40000000-0000-0000-0000-000000000001','30000000-0000-0000-0000-000000000001',
                            'f0000000-0000-0000-0000-000000000001',1,'Mensagem de antes','mensagem de antes',current_timestamp)
                    """);
            sql.executeUpdate("""
                    insert into receita (id,versao,titulo,titulo_chave,descricao)
                    values ('50000000-0000-0000-0000-000000000001',0,'','','')
                    """);

            Flyway.configure().dataSource(banco, "sa", "").load().migrate();
            try (var dados = sql.executeQuery("""
                    select dm_ativa, dm_primeiro, m.texto, r.titulo, r.titulo_chave
                    from chat_conversa c join chat_mensagem m on m.conversa_id = c.id cross join receita r
                    """)) {
                assertThat(dados.next()).isTrue();
                assertThat(dados.getBoolean("dm_ativa")).isFalse();
                assertThat(dados.getString("dm_primeiro")).isEqualTo("10000000-0000-0000-0000-000000000001");
                assertThat(dados.getString("texto")).isEqualTo("Mensagem de antes");
                assertThat(dados.getString("titulo")).isEqualTo("Receita sem título");
                assertThat(dados.getString("titulo_chave")).isEqualTo("receita sem titulo");
            }
        }
    }
}
