package dev.pedrocaldas.receitopedia.aplicacao.comunicacao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DadosChat {
    public static final String ENVIADA = "mensagem.enviada";
    public static final String ALTERADA = "conversa.alterada";

    public static final String DM_CRIADA = "dm.criada";
    public static final String DM_DESATIVADA = "dm.desativada";
    public static final String DM_REATIVADA = "dm.reativada";

    public static boolean eventoDm(String tipo) {
        return DM_CRIADA.equals(tipo) || DM_DESATIVADA.equals(tipo) || DM_REATIVADA.equals(tipo);
    }

    private DadosChat() {
    }

    public record Conversa(UUID id, String tipo, String nome, UUID outroId, boolean bloqueada, boolean bloqueioMeu,
            String ultimaMensagem, long naoLidas) {}

    public record Mensagem(UUID id, long sequencia, UUID autorId, String autor, String texto, long versao,
            Instant enviadaEm, boolean editada, boolean apagada, String status, List<Anexo> anexos) {}

    public enum AcaoGrupo { REMOVER, PROMOVER, SAIR }

    public record Grupo(UUID id, String nome, List<Membro> membros) {}

    public record Membro(UUID id, String nome, boolean admin) {}

    public record Arquivo(String mime, byte[] bytes) {}

    public record Anexo(UUID id, String tipo, boolean miniatura) {}
}
