package dev.pedrocaldas.receitopedia.aplicacao.receitas;

public class CatalogoAlterado extends RuntimeException {
    public CatalogoAlterado() {
        super("A lista de categorias mudou. Tente reordenar novamente.");
    }
}
