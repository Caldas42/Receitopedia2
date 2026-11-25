package dev.pedrocaldas.receitopedia.dominio.evento;

public interface EventoObservador<E> {
	void observarEvento(E evento);
}