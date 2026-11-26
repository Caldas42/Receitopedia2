package dev.pedrocaldas.receitopedia.dominio.receitas;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SelecaoCategorias(List<UUID> ids) {
    public SelecaoCategorias {
        if (ids.size() > 5) {
            throw new IllegalArgumentException("Selecione até 5 categorias por receita.");
        }
        if (ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Selecione categorias válidas e sem repetição.");
        }
        ids = List.copyOf(ids);
    }

    public void conferirExistentes(int encontradas) {
        if (encontradas != ids.size()) {
            throw new IllegalArgumentException("Uma ou mais categorias selecionadas não existem.");
        }
    }
}
