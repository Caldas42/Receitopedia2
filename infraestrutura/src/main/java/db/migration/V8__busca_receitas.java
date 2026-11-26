package db.migration;

import java.sql.Connection;
import java.sql.SQLException;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import dev.pedrocaldas.receitopedia.dominio.receitas.TextosReceitas;

public class V8__busca_receitas extends BaseJavaMigration {
    @Override
    public void migrate(Context contexto) throws SQLException {
        Connection conexao = contexto.getConnection();
        try (var ddl = conexao.createStatement()) {
            ddl.execute("ALTER TABLE receita ADD COLUMN titulo_chave VARCHAR(240)");
            preencherChaves(conexao);
            ddl.execute("ALTER TABLE receita ALTER COLUMN titulo_chave SET NOT NULL");
        }
    }

    private void preencherChaves(Connection conexao) throws SQLException {
        try (var consulta = conexao.prepareStatement("SELECT id, titulo FROM receita");
                var atualizacao = conexao.prepareStatement("UPDATE receita SET titulo_chave = ? WHERE id = ?")) {
            consulta.setFetchSize(500);
            try (var receitas = consulta.executeQuery()) {
                int noLote = 0;
                while (receitas.next()) {
                    atualizacao.setString(1, TextosReceitas.normalizar(receitas.getString("titulo")));
                    atualizacao.setObject(2, receitas.getObject("id"));
                    atualizacao.addBatch();
                    if (++noLote == 500) {
                        atualizacao.executeBatch();
                        noLote = 0;
                    }
                }
                if (noLote != 0) {
                    atualizacao.executeBatch();
                }
            }
        }
    }
}
