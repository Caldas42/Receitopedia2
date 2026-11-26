package dev.pedrocaldas.receitopedia.aplicacao.receitas;

public class CategoriaDuplicada extends RuntimeException {
    public CategoriaDuplicada() {
        super("Já existe uma categoria com esse nome.");
    }
}
