package dev.pedrocaldas.receitopedia.apresentacao.comunicacao;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.ChatServico;
import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.ConversasRepositorio;

class ChatBuscaTest {
    private final ConversasRepositorio repositorio = mock(ConversasRepositorio.class);
    private final ChatServico chat = new ChatServico(repositorio);
    private final UUID ator = UUID.randomUUID();
    private final UUID conversa = UUID.randomUUID();

    @Test
    void normalizaAcentosEEscapaMetacaracteresComoTextoLiteral() {
        chat.historico(ator, conversa, "  PÃO_%!  ", 30);
        verify(repositorio).historico(ator, conversa, "%pao!_!%!!%", 30);
    }

    @Test
    void rejeitaBuscaLongaEPaginacaoInvalidaAntesDaConsulta() {
        assertThrows(IllegalArgumentException.class, () -> chat.historico(ator, conversa, "a".repeat(101), 30));
        assertThrows(IllegalArgumentException.class, () -> chat.historico(ator, conversa, "pão", 0));
        verifyNoInteractions(repositorio);
    }
}
