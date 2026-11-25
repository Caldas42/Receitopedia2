package dev.pedrocaldas.receitopedia.aplicacao.receitas;

public class ReceitaNaoEncontrada extends RuntimeException {
    public ReceitaNaoEncontrada() {
        super("Receita não encontrada.");
    }
}
