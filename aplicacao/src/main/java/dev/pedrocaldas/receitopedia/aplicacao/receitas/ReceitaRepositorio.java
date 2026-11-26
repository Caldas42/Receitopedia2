package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.Optional;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.DadosReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.Receita;
import dev.pedrocaldas.receitopedia.dominio.receitas.SelecaoCategorias;

public interface ReceitaRepositorio {
    Receita adicionar(Receita receita, AlteracaoFotos fotos, SelecaoCategorias categorias);

    PaginaReceitas buscar(UUID donoId, BuscaReceitas busca);

    Optional<Receita> encontrar(UUID id);

    Receita editar(UUID id, long versao, UUID usuarioId, DadosReceita dados, AlteracaoFotos fotos, SelecaoCategorias categorias);

    void remover(UUID id, long versao, UUID usuarioId);
}
