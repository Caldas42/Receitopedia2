package dev.pedrocaldas.receitopedia.infraestrutura.fotos;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import dev.pedrocaldas.receitopedia.dominio.receitas.FotoReceita;

@Component
public class LimpezaFotos {
    private static final Logger LOG = LoggerFactory.getLogger(LimpezaFotos.class);
    private final JdbcTemplate jdbc;
    private final FotosEmDisco disco;
    private final TransactionTemplate novaTransacao;

    public LimpezaFotos(JdbcTemplate jdbc, FotosEmDisco disco, PlatformTransactionManager transacoes) {
        this.jdbc = jdbc;
        this.disco = disco;
        novaTransacao = new TransactionTemplate(transacoes);
        novaTransacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void removerAposCommit(List<FotoReceita> removidas) {
        if (removidas.isEmpty()) {
            return;
        }
        for (FotoReceita foto : removidas) {
            jdbc.update("insert into foto_exclusao_pendente (id) values (?)", foto.id());
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                limparPendentes();
            }
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void limparPendentes() {
        try {
            novaTransacao.executeWithoutResult(status -> {
                var pendentes = jdbc.query("select id from foto_exclusao_pendente",
                        (linha, numero) -> linha.getObject(1, UUID.class));
                for (UUID id : pendentes) {
                    try {
                        disco.apagar(id);
                        jdbc.update("delete from foto_exclusao_pendente where id = ?", id);
                    } catch (IOException e) {
                        LOG.warn("A exclusão da foto {} continua pendente", id, e);
                    }
                }
            });
        } catch (RuntimeException e) {
            LOG.warn("Não foi possível processar as exclusões pendentes de fotos", e);
        }
    }
}
