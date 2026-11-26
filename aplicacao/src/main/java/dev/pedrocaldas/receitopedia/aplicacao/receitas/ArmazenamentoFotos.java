package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.Optional;
import java.util.UUID;

public interface ArmazenamentoFotos {
    Optional<byte[]> ler(UUID id);
}
