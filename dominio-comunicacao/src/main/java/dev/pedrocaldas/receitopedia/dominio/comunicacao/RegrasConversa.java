package dev.pedrocaldas.receitopedia.dominio.comunicacao;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class RegrasConversa {
    private RegrasConversa() {
    }

    public static void conferirBloqueio(boolean bloqueada) {
        if (bloqueada) {
            throw new OperacaoProibida("As mensagens diretas entre estas contas estão bloqueadas.");
        }
    }

    public static void conferirAlteracao(UUID ator, UUID autor, Instant envio, Instant agora, boolean apagada) {
        if (!autor.equals(ator)) {
            throw new OperacaoProibida("Somente o autor pode alterar a mensagem.");
        }
        if (apagada || agora.isAfter(envio.plus(Duration.ofMinutes(15)))) {
            throw new OperacaoProibida("A mensagem só pode ser alterada ou apagada em até 15 minutos.");
        }
    }

    public static String nomeGrupo(String nome) {
        String limpo = nome == null ? "" : nome.strip();
        if (limpo.isBlank() || limpo.codePointCount(0, limpo.length()) > 80) {
            throw new IllegalArgumentException("Informe um nome de grupo de 1 a 80 caracteres.");
        }
        return limpo;
    }

    public static void conferirCapacidade(long participantes) {
        if (participantes > 256) {
            throw new ConflitoMensagem("O grupo já atingiu o limite de 256 participantes.");
        }
    }

    public static void exigirAdmin(boolean administrador) {
        if (!administrador) {
            throw new OperacaoProibida("Somente administradores do grupo podem fazer esta alteração.");
        }
    }

    public static void manterAdmin(boolean removendoAdmin, long administradores) {
        if (removendoAdmin && administradores <= 1) {
            throw new ConflitoMensagem("Promova outro administrador antes de remover o último.");
        }
    }
}
