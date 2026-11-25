package dev.pedrocaldas.receitopedia.dominio.receitas;

import java.util.UUID;

public record Receita(UUID id, long versao, DadosReceita dados) {
    public static Receita nova(DadosReceita dados) {
        return new Receita(UUID.randomUUID(), 0, dados);
    }
}
