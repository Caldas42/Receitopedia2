package dev.pedrocaldas.receitopedia.dominio.receitas;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record OrdemCategorias(List<UUID> ids) {
    public OrdemCategorias {
        if (ids == null || ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Informe uma ordem sem categorias repetidas.");
        }
        ids = List.copyOf(ids);
    }

    public boolean correspondeAoCatalogo(Set<UUID> existentes) {
        return existentes.size() == ids.size() && existentes.containsAll(ids);
    }
}
