package dev.pedrocaldas.receitopedia.dominio.comunicacao;

public class ConflitoMensagem extends RuntimeException {
    public ConflitoMensagem(String mensagem) {
        super(mensagem);
    }
}
