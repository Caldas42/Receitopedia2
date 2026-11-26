package dev.pedrocaldas.receitopedia.aplicacao.receitas;

public class CategoriaNaoEncontrada extends RuntimeException {
    public CategoriaNaoEncontrada() {
        super("Categoria não encontrada.");
    }
}
