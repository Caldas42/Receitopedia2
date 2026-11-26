package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.List;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.Categoria;
import dev.pedrocaldas.receitopedia.dominio.receitas.OrdemCategorias;

public class CategoriaServicoAplicacao {
    private final CategoriaRepositorio repositorio;

    public CategoriaServicoAplicacao(CategoriaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public Categoria adicionar(String nome) {
        return repositorio.adicionar(Categoria.nova(nome));
    }

    public Categoria editar(UUID id, String nome) {
        return repositorio.editar(new Categoria(id, nome));
    }

    public void remover(UUID id) {
        repositorio.remover(id);
    }

    public List<Categoria> reordenar(List<UUID> ids) {
        return repositorio.reordenar(new OrdemCategorias(ids));
    }

    public List<Categoria> listar() {
        return repositorio.listar();
    }
}
