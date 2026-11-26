package dev.pedrocaldas.receitopedia.aplicacao.receitas;

public class FotoNaoEncontrada extends RuntimeException {
    public FotoNaoEncontrada() {
        super("Foto não encontrada.");
    }
}
