package dev.pedrocaldas.receitopedia.dominio.comunicacao;

public class OperacaoProibida extends RuntimeException {
    public OperacaoProibida(String mensagem) {
        super(mensagem);
    }
}
