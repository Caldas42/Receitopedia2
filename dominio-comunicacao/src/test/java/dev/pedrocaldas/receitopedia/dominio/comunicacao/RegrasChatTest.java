package dev.pedrocaldas.receitopedia.dominio.comunicacao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RegrasChatTest {
    @Test
    void validaTextoEAnexos() {
        assertEquals("Olá", new ConteudoMensagem("  Olá  ", 0).texto());
        assertEquals("", new ConteudoMensagem("  ", 1).texto());
        assertDoesNotThrow(() -> new ConteudoMensagem("é".repeat(2000), 0));
        assertThrows(IllegalArgumentException.class, () -> new ConteudoMensagem("  ", 0));
        assertThrows(IllegalArgumentException.class, () -> new ConteudoMensagem("a".repeat(2001), 0));
        assertThrows(IllegalArgumentException.class, () -> new ConteudoMensagem("texto", 5));
    }

    @Test
    void aplicaCapacidadeEAdmin() {
        assertDoesNotThrow(() -> RegrasConversa.conferirCapacidade(256));
        assertThrows(ConflitoMensagem.class, () -> RegrasConversa.conferirCapacidade(257));
        assertThrows(OperacaoProibida.class, () -> RegrasConversa.exigirAdmin(false));
        assertThrows(ConflitoMensagem.class, () -> RegrasConversa.manterAdmin(true, 1));
        assertDoesNotThrow(() -> RegrasConversa.manterAdmin(true, 2));
    }

    @Test
    void exigeTodasAsLeiturasEImpedeEnvioBloqueado() {
        assertEquals(EstadoMensagem.ENVIADA, EstadoMensagem.calcular(false, true));
        assertEquals(EstadoMensagem.ENTREGUE, EstadoMensagem.calcular(true, true));
        assertEquals(EstadoMensagem.LIDA, EstadoMensagem.calcular(true, false));
        assertThrows(OperacaoProibida.class, () -> RegrasConversa.conferirBloqueio(true));
    }

    @Test
    void restringeAlteracaoAoAutorEDentroDoPrazo() {
        var autor = UUID.randomUUID();
        var envio = Instant.parse("2026-01-01T12:00:00Z");
        assertDoesNotThrow(() -> RegrasConversa.conferirAlteracao(autor, autor, envio, envio.plusSeconds(900), false));
        assertThrows(OperacaoProibida.class,
                () -> RegrasConversa.conferirAlteracao(autor, autor, envio, envio.plusSeconds(901), false));
        assertThrows(OperacaoProibida.class,
                () -> RegrasConversa.conferirAlteracao(UUID.randomUUID(), autor, envio, envio, false));
        assertThrows(OperacaoProibida.class,
                () -> RegrasConversa.conferirAlteracao(autor, autor, envio, envio, true));
    }
}
