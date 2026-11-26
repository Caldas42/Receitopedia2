package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.DadosReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.Receita;

public interface ReceitaRepositorio {
    Receita adicionar(Receita receita);

    List<ReceitaResumo> listar();

    List<ReceitaResumo> listarDoDono(UUID donoId);

    Optional<Receita> encontrar(UUID id);

    Receita editar(UUID id, long versao, UUID usuarioId, DadosReceita dados);

    void remover(UUID id, long versao, UUID usuarioId);
}
