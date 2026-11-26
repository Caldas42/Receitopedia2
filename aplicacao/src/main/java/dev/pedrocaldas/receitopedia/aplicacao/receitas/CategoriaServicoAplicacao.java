package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.List;

import dev.pedrocaldas.receitopedia.dominio.receitas.Categoria;

public class CategoriaServicoAplicacao {
    private final CategoriaRepositorio repositorio;

    public CategoriaServicoAplicacao(CategoriaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public Categoria adicionar(String nome) {
        return repositorio.adicionar(Categoria.nova(nome));
    }

    public List<Categoria> listar() {
        return repositorio.listar();
    }
}
