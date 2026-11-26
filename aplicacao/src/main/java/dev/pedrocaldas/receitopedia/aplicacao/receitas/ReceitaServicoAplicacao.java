package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.List;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.AutorReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.DadosReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.Receita;

public class ReceitaServicoAplicacao {
    private final ReceitaRepositorio repositorio;

    public ReceitaServicoAplicacao(ReceitaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public Receita adicionar(DadosReceita dados, AutorReceita dono) {
        return repositorio.adicionar(Receita.nova(dados, dono));
    }

    public List<ReceitaResumo> listar() {
        return repositorio.listar();
    }

    public Receita visualizar(UUID id) {
        return repositorio.encontrar(id).orElseThrow(ReceitaNaoEncontrada::new);
    }

    public Receita editar(UUID id, Long versao, UUID usuarioId, DadosReceita dados) {
        return repositorio.editar(id, validarVersao(versao), usuarioId, dados);
    }

    public void remover(UUID id, Long versao, UUID usuarioId) {
        repositorio.remover(id, validarVersao(versao), usuarioId);
    }

    private static long validarVersao(Long versao) {
        if (versao == null || versao < 0) {
            throw new IllegalArgumentException("Informe uma versão válida da receita.");
        }
        return versao;
    }
}
