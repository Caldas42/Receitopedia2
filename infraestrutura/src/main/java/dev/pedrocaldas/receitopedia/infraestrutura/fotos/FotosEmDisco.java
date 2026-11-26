package dev.pedrocaldas.receitopedia.infraestrutura.fotos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import dev.pedrocaldas.receitopedia.aplicacao.receitas.ArmazenamentoFotos;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.NovaFoto;
import dev.pedrocaldas.receitopedia.dominio.receitas.TipoFoto;

@Component
public class FotosEmDisco implements ArmazenamentoFotos {
    private static final Logger LOG = LoggerFactory.getLogger(FotosEmDisco.class);
    private final Path diretorio;

    public FotosEmDisco(@Value("${receitopedia.fotos.diretorio:./dados/fotos}") String diretorio) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    public void gravar(List<NovaFoto> novas) {
        if (novas.isEmpty()) {
            return;
        }

        var gravadas = new ArrayList<UUID>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    desfazer(gravadas);
                }
            }
        });
        try {
            Files.createDirectories(diretorio);
            for (NovaFoto nova : novas) {
                UUID id = nova.foto().id();
                try (var saida = Files.newOutputStream(arquivo(id), StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE)) {
                    gravadas.add(id);
                    saida.write(nova.bytes());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível armazenar as fotos.", e);
        }
    }

    @Override
    public Optional<byte[]> ler(UUID id) {
        try (var entrada = Files.newInputStream(arquivo(id), LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = entrada.readNBytes(TipoFoto.LIMITE_BYTES + 1);
            return bytes.length <= TipoFoto.LIMITE_BYTES ? Optional.of(bytes) : Optional.empty();
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler a foto.", e);
        }
    }

    void apagar(UUID id) throws IOException {
        Files.deleteIfExists(arquivo(id));
    }

    private Path arquivo(UUID id) {
        return diretorio.resolve(id.toString());
    }

    private void desfazer(List<UUID> fotos) {
        for (UUID id : fotos) {
            try {
                apagar(id);
            } catch (IOException e) {
                LOG.warn("Não foi possível desfazer a gravação da foto {}", id, e);
            }
        }
    }
}
