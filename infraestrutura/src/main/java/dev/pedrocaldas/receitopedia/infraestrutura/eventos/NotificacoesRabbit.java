package dev.pedrocaldas.receitopedia.infraestrutura.eventos;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.pedrocaldas.receitopedia.aplicacao.eventos.Evento;
import dev.pedrocaldas.receitopedia.aplicacao.eventos.ObservadorEvento;

@Component
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "receitopedia.eventos.ativo", havingValue = "true")
class NotificacoesRabbit {
    private final ObjectMapper json;
    private final ObservadorEvento observador;

    NotificacoesRabbit(ObjectMapper json, ObservadorEvento observador) {
        this.json = json;
        this.observador = observador;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(exclusive = "true", autoDelete = "true"),
            exchange = @Exchange(value = TopologiaEventos.NOTIFICACOES, type = "fanout")))
    void receber(byte[] dados) throws Exception {
        observador.notificar(json.readValue(dados, Evento.class));
    }
}
