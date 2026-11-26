package dev.pedrocaldas.receitopedia.dominio.comunicacao;

public class ConversaIndisponivel extends RuntimeException {
    public ConversaIndisponivel() {
        super("Conversa ou mensagem indisponível.");
    }
}
