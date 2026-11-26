package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.List;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.SelecaoFotos;

public record AlteracaoFotos(SelecaoFotos selecao, List<NovaFoto> uploads) {
    public static AlteracaoFotos preparar(List<UUID> mantidas, List<byte[]> arquivos, Integer principal) {
        SelecaoFotos.validarQuantidade(mantidas.size() + arquivos.size());
        var uploads = arquivos.stream().map(NovaFoto::de).toList();
        return new AlteracaoFotos(
                new SelecaoFotos(mantidas, uploads.stream().map(NovaFoto::foto).toList(), principal), uploads);
    }
}
