package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.List;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.Categoria;

public record ReceitaResumo(UUID id, String titulo, String descricao, Integer tempoMinutos, UUID fotoPrincipalId, String autor,
        List<Categoria> categorias) {
    public ReceitaResumo(UUID id, String titulo, String descricao, Integer tempoMinutos, UUID fotoPrincipalId, String autor) {
        this(id, titulo, descricao, tempoMinutos, fotoPrincipalId, autor, List.of());
    }

    public ReceitaResumo comCategorias(List<Categoria> categorias) {
        return new ReceitaResumo(id, titulo, descricao, tempoMinutos, fotoPrincipalId, autor, categorias);
    }
}
