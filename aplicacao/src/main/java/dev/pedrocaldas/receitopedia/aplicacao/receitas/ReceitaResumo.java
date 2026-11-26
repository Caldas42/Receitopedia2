package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.UUID;

public record ReceitaResumo(UUID id, String titulo, String descricao, Integer tempoMinutos, UUID fotoPrincipalId) {
}
