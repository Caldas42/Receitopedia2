package db.migration;

import java.sql.SQLException;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import dev.pedrocaldas.receitopedia.dominio.receitas.TextosReceitas;

public class V13__titulo_obrigatorio extends BaseJavaMigration {
    @Override
    public void migrate(Context contexto) throws SQLException {
        var conexao = contexto.getConnection();
        String padrao = "Receita sem título";
        try (var consulta = conexao.prepareStatement("SELECT id, titulo FROM receita");
                var receitas = consulta.executeQuery();
                var atualizacao = conexao.prepareStatement("UPDATE receita SET titulo = ?, titulo_chave = ? WHERE id = ?")) {
            while (receitas.next()) {
                if (receitas.getString("titulo").isBlank()) {
                    atualizacao.setString(1, padrao);
                    atualizacao.setString(2, TextosReceitas.normalizar(padrao));
                    atualizacao.setObject(3, receitas.getObject("id"));
                    atualizacao.addBatch();
                }
            }
            atualizacao.executeBatch();
        }
        try (var ddl = conexao.createStatement()) {
            ddl.execute("ALTER TABLE receita ADD CONSTRAINT ck_receita_titulo_preenchido CHECK (TRIM(titulo) <> '')");
        }
    }
}
