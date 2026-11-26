package dev.pedrocaldas.receitopedia.apresentacao.pessoas;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.pedrocaldas.receitopedia.aplicacao.pessoas.PessoasRepositorio;
import dev.pedrocaldas.receitopedia.aplicacao.pessoas.PessoasRepositorio.Lista;
import dev.pedrocaldas.receitopedia.aplicacao.pessoas.PessoasServico;

class PessoasServicoTest {
    private final UUID ator = UUID.randomUUID();
    private final PessoasRepositorio repositorio = mock(PessoasRepositorio.class);
    private final PessoasServico pessoas = new PessoasServico(repositorio);

    @Test
    void rejeitaAutoSeguimentoAntesDeAcessarOBanco() {
        assertThrows(IllegalArgumentException.class, () -> pessoas.alterarSeguimento(ator, ator, true));
        verifyNoInteractions(repositorio);
    }

    @Test
    void normalizaBuscaETrataMetacaracteresComoTexto() {
        pessoas.buscar(ator, Lista.AMIGOS, "  PÃO_%!  ", "cursor");
        verify(repositorio).buscar(ator, Lista.AMIGOS, "%pao!_!%!!%", "cursor", 26);
    }
}
