package dev.pedrocaldas.receitopedia.aplicacao.eventos;

public interface ProcessadorEvento {
    boolean atende(String tipo);

    void processar(Evento evento);
}
