package dev.pedrocaldas.receitopedia.aplicacao.eventos;

import java.util.UUID;

public record Evento(UUID id, String tipo, UUID agregado, UUID referencia) {
    public static Evento novo(String tipo, UUID agregado, UUID referencia) {
        return new Evento(UUID.randomUUID(), tipo, agregado, referencia);
    }
}
