package dev.pedrocaldas.receitopedia.dominio.receitas;

import java.util.List;
import java.util.UUID;

public record Receita(UUID id, long versao, DadosReceita dados, AutorReceita dono, List<FotoReceita> fotos) {
    public static Receita nova(DadosReceita dados, AutorReceita dono) {
        return new Receita(UUID.randomUUID(), 0, dados, dono, List.of());
    }
}
