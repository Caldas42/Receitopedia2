package dev.pedrocaldas.receitopedia.dominio.receitas;

public class ReceitaAcessoNegado extends RuntimeException {
    public ReceitaAcessoNegado() {
        super("Somente o dono pode alterar ou remover esta receita.");
    }
}
