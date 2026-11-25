package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.List;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.DadosReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.Receita;

public class ReceitaServicoAplicacao {
    private final ReceitaRepositorio repositorio;

    public ReceitaServicoAplicacao(ReceitaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public Receita adicionar(DadosReceita dados) {
        return repositorio.adicionar(Receita.nova(dados));
    }

    public List<ReceitaResumo> listar() {
        return repositorio.listar();
    }

    public Receita visualizar(UUID id) {
        return repositorio.encontrar(id).orElseThrow(ReceitaNaoEncontrada::new);
    }

    public Receita editar(UUID id, Long versao, DadosReceita dados) {
        return repositorio.editar(id, validarVersao(versao), dados);
    }

    public void remover(UUID id, Long versao) {
        repositorio.remover(id, validarVersao(versao));
    }

    private static long validarVersao(Long versao) {
        if (versao == null || versao < 0) {
            throw new IllegalArgumentException("Informe uma versão válida da receita.");
        }
        return versao;
    }
}
