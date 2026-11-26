package dev.pedrocaldas.receitopedia.infraestrutura.eventos;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.pedrocaldas.receitopedia.aplicacao.eventos.Evento;

@Component
@ConditionalOnProperty(name = "receitopedia.eventos.ativo", havingValue = "true")
class PublicacaoRabbit {
    private final RabbitTemplate rabbit;
    private final ObjectMapper json;

    PublicacaoRabbit(RabbitTemplate rabbit, ObjectMapper json) {
        this.rabbit = rabbit;
        this.json = json;
    }

    Message mensagem(Evento evento) throws Exception {
        return MessageBuilder.withBody(json.writeValueAsBytes(evento)).setContentType("application/json")
                .setMessageId(evento.id().toString()).setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
    }

    void confirmar(String exchange, String chave, Message mensagem) throws Exception {
        var confirmacao = new CorrelationData(UUID.randomUUID().toString());
        rabbit.send(exchange, chave, mensagem, confirmacao);
        var resultado = confirmacao.getFuture().get(4, TimeUnit.SECONDS);
        if (!resultado.isAck() || (confirmacao.getReturned() != null && !TopologiaEventos.NOTIFICACOES.equals(exchange))) {
            throw new IllegalStateException("RabbitMQ não confirmou a rota " + exchange + "/" + chave);
        }
    }
}
