package dev.pedrocaldas.receitopedia.dominio.comunicacao;

public enum EstadoMensagem {
    ENVIADA, ENTREGUE, LIDA;

    public static EstadoMensagem calcular(boolean processada, boolean leituraPendente) {
        if (!processada) {
            return ENVIADA;
        }
        return leituraPendente ? ENTREGUE : LIDA;
    }
}
