package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.List;

import dev.pedrocaldas.receitopedia.dominio.receitas.Categoria;

public interface CategoriaRepositorio {
    Categoria adicionar(Categoria categoria);

    List<Categoria> listar();
}
