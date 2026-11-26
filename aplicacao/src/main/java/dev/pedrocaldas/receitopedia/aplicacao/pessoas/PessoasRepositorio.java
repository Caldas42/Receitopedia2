package dev.pedrocaldas.receitopedia.aplicacao.pessoas;

import java.util.List;
import java.util.UUID;

public interface PessoasRepositorio {
    boolean segue(UUID seguidor, UUID seguido);

    void alterarSeguimento(UUID seguidor, UUID seguido, boolean seguir);

    List<Pessoa> buscar(UUID ator, Lista lista, String trecho, String cursor, int limite);

    enum Lista { SEGUINDO, TODOS, AMIGOS }

    record Pessoa(UUID id, String nomeUsuario, String cursor) {}
}
