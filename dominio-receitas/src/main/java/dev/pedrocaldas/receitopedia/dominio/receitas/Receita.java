package dev.pedrocaldas.receitopedia.dominio.receitas;

import java.util.UUID;

public record Receita(UUID id, long versao, DadosReceita dados, AutorReceita dono) {
    public static Receita nova(DadosReceita dados, AutorReceita dono) {
        return new Receita(UUID.randomUUID(), 0, dados, dono);
    }
}
