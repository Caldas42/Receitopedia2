package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.FotoReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.TipoFoto;

public record NovaFoto(FotoReceita foto, byte[] bytes) {
    static NovaFoto de(byte[] bytes) {
        return new NovaFoto(new FotoReceita(UUID.randomUUID(), TipoFoto.detectar(bytes)), bytes);
    }
}
