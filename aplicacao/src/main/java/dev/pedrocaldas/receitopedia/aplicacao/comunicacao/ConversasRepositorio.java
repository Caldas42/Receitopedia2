package dev.pedrocaldas.receitopedia.aplicacao.comunicacao;

import java.util.List;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.Conversa;
import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.Mensagem;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.NovaFoto;
import dev.pedrocaldas.receitopedia.dominio.comunicacao.ConteudoMensagem;

public interface ConversasRepositorio {
    List<Conversa> listar(UUID ator);

    Mensagem enviar(UUID ator, UUID conversa, UUID id, ConteudoMensagem conteudo, List<NovaFoto> anexos);

    List<Mensagem> historico(UUID ator, UUID conversa, String trecho, long antes);

    void ler(UUID ator, UUID conversa, List<UUID> ids);

    void bloquear(UUID ator, UUID alvo, boolean bloquear);

    UUID criarGrupo(UUID ator, String nome, List<String> usuarios);

    DadosChat.Grupo grupo(UUID ator, UUID conversa);

    void renomearGrupo(UUID ator, UUID conversa, String nome);

    void membro(UUID ator, UUID conversa, DadosChat.AcaoGrupo acao, String nome, UUID alvo);

    Mensagem alterar(UUID ator, UUID conversa, UUID mensagem, long versao, String texto, boolean apagar);

    DadosChat.Arquivo anexo(UUID ator, UUID conversa, UUID id, boolean miniatura);

    List<UUID> participantes(UUID conversa);
}
