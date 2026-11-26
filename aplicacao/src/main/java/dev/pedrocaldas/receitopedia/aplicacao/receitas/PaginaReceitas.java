package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.List;

public record PaginaReceitas(List<ReceitaResumo> receitas, boolean temProxima) {
}
