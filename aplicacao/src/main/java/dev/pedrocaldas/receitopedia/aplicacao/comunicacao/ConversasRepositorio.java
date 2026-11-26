package dev.pedrocaldas.receitopedia.aplicacao.comunicacao;

import java.util.List;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.Conversa;
import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.Mensagem;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.NovaFoto;
import dev.pedrocaldas.receitopedia.dominio.comunicacao.ConteudoMensagem;

public interface ConversasRepositorio {
    UUID abrirDm(UUID ator, String nomeChave);

    List<Conversa> listar(UUID ator);

    Mensagem enviar(UUID ator, UUID conversa, UUID id, ConteudoMensagem conteudo, List<NovaFoto> anexos);

    List<Mensagem> historico(UUID ator, UUID conversa, String trecho, long antes);

    void ler(UUID ator, UUID conversa, List<UUID> ids);

    void bloquear(UUID ator, UUID alvo, boolean bloquear);

    List<UUID> participantes(UUID conversa);
}
