package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.List;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.Categoria;
import dev.pedrocaldas.receitopedia.dominio.receitas.OrdemCategorias;

public interface CategoriaRepositorio {
    Categoria adicionar(Categoria categoria);

    Categoria editar(Categoria categoria);

    void remover(UUID id);

    List<Categoria> reordenar(OrdemCategorias ordem);

    List<Categoria> listar();
}
