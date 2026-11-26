package dev.pedrocaldas.receitopedia.aplicacao.comunicacao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DadosChat {
    public static final String ENVIADA = "mensagem.enviada";
    public static final String ALTERADA = "conversa.alterada";

    private DadosChat() {
    }

    public record Conversa(UUID id, String tipo, String nome, UUID outroId, boolean bloqueada,
            String ultimaMensagem, long naoLidas) {}

    public record Mensagem(UUID id, long sequencia, UUID autorId, String autor, String texto, long versao,
            Instant enviadaEm, boolean editada, boolean apagada, String status, List<Anexo> anexos) {}

    public record Anexo(UUID id, String tipo, boolean miniatura) {}
}
