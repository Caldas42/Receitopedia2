package dev.pedrocaldas.receitopedia.aplicacao.receitas;

public class ReceitaDesatualizada extends RuntimeException {
    public ReceitaDesatualizada() {
        super("A receita foi alterada. Abra a versão atual antes de tentar novamente.");
    }
}
