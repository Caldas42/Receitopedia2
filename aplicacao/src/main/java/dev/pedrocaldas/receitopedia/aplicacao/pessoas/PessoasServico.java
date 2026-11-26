package dev.pedrocaldas.receitopedia.aplicacao.pessoas;

import java.util.List;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.TextosReceitas;

public class PessoasServico {
    public static final String SEGUIMENTO_ALTERADO = "seguimento.alterado";
    private static final int PAGINA = 25;
    private final PessoasRepositorio repositorio;

    public PessoasServico(PessoasRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public void alterarSeguimento(UUID ator, UUID alvo, boolean seguir) {
        if (ator.equals(alvo)) {
            throw new IllegalArgumentException("Você não pode seguir a si mesmo.");
        }
        repositorio.alterarSeguimento(ator, alvo, seguir);
    }

    public boolean segue(UUID ator, UUID alvo) {
        return repositorio.segue(ator, alvo);
    }

    public Pagina buscar(UUID ator, PessoasRepositorio.Lista lista, String busca, String cursor) {
        String termo = busca.strip();
        if (termo.length() > 100 || cursor.length() > 20) {
            throw new IllegalArgumentException("Informe uma busca de até 100 caracteres e um cursor válido.");
        }
        String literal = TextosReceitas.normalizar(termo).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        var resultados = repositorio.buscar(ator, lista, "%" + literal + "%", cursor, PAGINA + 1);
        var itens = resultados.size() > PAGINA ? resultados.subList(0, PAGINA) : resultados;
        var proximo = resultados.size() > PAGINA ? itens.get(PAGINA - 1).cursor() : null;
        return new Pagina(itens, proximo);
    }

    public record Pagina(List<PessoasRepositorio.Pessoa> pessoas, String proximo) {}
}
