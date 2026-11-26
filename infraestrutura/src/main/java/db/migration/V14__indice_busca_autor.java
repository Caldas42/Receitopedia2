package db.migration;

import java.sql.SQLException;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V14__indice_busca_autor extends BaseJavaMigration {
    @Override
    public void migrate(Context contexto) throws SQLException {
        var conexao = contexto.getConnection();
        if ("PostgreSQL".equals(conexao.getMetaData().getDatabaseProductName())) {
            try (var ddl = conexao.createStatement()) {
                ddl.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
                ddl.execute("CREATE INDEX ix_usuario_nome_trechos ON usuario USING gin (nome_chave gin_trgm_ops)");
            }
        }
    }
}
