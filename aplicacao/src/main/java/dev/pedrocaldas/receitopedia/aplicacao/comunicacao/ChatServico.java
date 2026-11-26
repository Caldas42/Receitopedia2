package dev.pedrocaldas.receitopedia.aplicacao.comunicacao;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.Conversa;
import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.Mensagem;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.AlteracaoFotos;
import dev.pedrocaldas.receitopedia.dominio.comunicacao.ConteudoMensagem;
import dev.pedrocaldas.receitopedia.dominio.comunicacao.RegrasConversa;
import dev.pedrocaldas.receitopedia.dominio.receitas.TextosReceitas;

public class ChatServico {
    public static final int PAGINA = 30;
    private final ConversasRepositorio repositorio;

    public ChatServico(ConversasRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public UUID abrirDm(UUID ator, String nome) {
        return repositorio.abrirDm(ator, nomeUsuario(nome));
    }

    public List<Conversa> listar(UUID ator) {
        return repositorio.listar(ator);
    }

    public Mensagem enviar(UUID ator, UUID conversa, UUID id, String texto, List<byte[]> arquivos) {
        if (id == null) {
            throw new IllegalArgumentException("Informe o identificador UUID da mensagem.");
        }
        var conteudo = new ConteudoMensagem(texto, arquivos.size());
        var anexos = AlteracaoFotos.preparar(List.of(), arquivos, null).uploads();
        return repositorio.enviar(ator, conversa, id, conteudo, anexos);
    }

    public List<Mensagem> historico(UUID ator, UUID conversa, String busca, long antes) {
        String termo = busca.strip();
        if (termo.codePointCount(0, termo.length()) > 100 || antes <= 0) {
            throw new IllegalArgumentException("Informe uma busca de até 100 caracteres e uma página válida.");
        }
        String literal = TextosReceitas.normalizar(termo).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return repositorio.historico(ator, conversa, "%" + literal + "%", antes);
    }

    public void ler(UUID ator, UUID conversa, List<UUID> ids) {
        if (ids == null || ids.size() > PAGINA || ids.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Informe até 30 identificadores de mensagem.");
        }
        repositorio.ler(ator, conversa, ids.stream().distinct().toList());
    }

    public void bloquear(UUID ator, UUID alvo, boolean bloquear) {
        if (ator.equals(alvo)) {
            throw new IllegalArgumentException("Selecione outro usuário.");
        }
        repositorio.bloquear(ator, alvo, bloquear);
    }

    public UUID criarGrupo(UUID ator, String nome, List<String> membros) {
        var usuarios = membros == null ? List.<String>of() : membros.stream().map(ChatServico::nomeUsuario).distinct().toList();
        RegrasConversa.conferirCapacidade(usuarios.size());
        return repositorio.criarGrupo(ator, RegrasConversa.nomeGrupo(nome), usuarios);
    }

    public DadosChat.Grupo grupo(UUID ator, UUID conversa) {
        return repositorio.grupo(ator, conversa);
    }

    public void renomearGrupo(UUID ator, UUID conversa, String nome) {
        repositorio.renomearGrupo(ator, conversa, RegrasConversa.nomeGrupo(nome));
    }

    public void membro(UUID ator, UUID conversa, DadosChat.AcaoGrupo acao, String nome, UUID alvo) {
        repositorio.membro(ator, conversa, acao, acao == DadosChat.AcaoGrupo.ADICIONAR ? nomeUsuario(nome) : null, alvo);
    }

    public Mensagem alterar(UUID ator, UUID conversa, UUID mensagem, Long versao, String texto, boolean apagar) {
        if (versao == null || versao < 0) {
            throw new IllegalArgumentException("Informe a versão atual da mensagem.");
        }
        return repositorio.alterar(ator, conversa, mensagem, versao, texto, apagar);
    }

    public DadosChat.Arquivo anexo(UUID ator, UUID conversa, UUID id, boolean miniatura) {
        return repositorio.anexo(ator, conversa, id, miniatura);
    }

    public static String nomeUsuario(String nome) {
        String chave = nome == null ? "" : nome.strip().toLowerCase(Locale.ROOT);
        if (chave.isEmpty() || chave.length() > 20) {
            throw new IllegalArgumentException("Informe um nome de usuário válido.");
        }
        return chave;
    }
}
