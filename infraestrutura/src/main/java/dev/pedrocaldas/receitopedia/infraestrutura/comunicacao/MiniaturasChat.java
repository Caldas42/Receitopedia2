package dev.pedrocaldas.receitopedia.infraestrutura.comunicacao;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.pedrocaldas.receitopedia.infraestrutura.fotos.FotosEmDisco;
import jakarta.annotation.PreDestroy;

@Component
class MiniaturasChat {
    private final FotosEmDisco arquivos;
    private final Path diretorio;
    private final ProcessadorImagens processador;

    MiniaturasChat(FotosEmDisco arquivos, @Value("${receitopedia.fotos.diretorio:./dados/fotos}") String diretorio,
            @Value("${RECEITOPEDIA_THREADS_IMAGENS:2}") int threads) {
        this.arquivos = arquivos;
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize().resolve("miniaturas-chat");
        processador = new ProcessadorImagens(threads);
    }

    List<UUID> preparar(List<UUID> anexos) {
        var entradas = anexos.stream().map(id -> arquivos.ler(id)
                .orElseThrow(() -> new IllegalStateException("Original do anexo não encontrado: " + id))).toList();
        var imagens = processador.gerar(entradas);
        var geradas = new java.util.ArrayList<UUID>();
        for (int i = 0; i < imagens.size(); i++) {
            if (imagens.get(i) != null) {
                gravar(anexos.get(i), imagens.get(i));
                geradas.add(anexos.get(i));
            }
        }
        return geradas;
    }

    Optional<byte[]> ler(UUID id) {
        try (var entrada = Files.newInputStream(diretorio.resolve(id.toString()), LinkOption.NOFOLLOW_LINKS)) {
            return Optional.of(entrada.readAllBytes());
        } catch (java.nio.file.NoSuchFileException erro) {
            return Optional.empty();
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }

    private void gravar(UUID id, byte[] bytes) {
        try {
            Files.createDirectories(diretorio);
            var temporario = Files.createTempFile(diretorio, "miniatura-", ".tmp");
            try {
                Files.write(temporario, bytes);
                Files.move(temporario, diretorio.resolve(id.toString()),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporario);
            }
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }

    @PreDestroy
    void encerrar() {
        processador.close();
    }
}
