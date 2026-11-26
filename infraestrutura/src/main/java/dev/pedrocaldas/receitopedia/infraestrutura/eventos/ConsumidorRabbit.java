package dev.pedrocaldas.receitopedia.infraestrutura.eventos;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import dev.pedrocaldas.receitopedia.aplicacao.eventos.Evento;

@Component
@ConditionalOnProperty(name = "receitopedia.eventos.worker", havingValue = "true")
class ConsumidorRabbit {
    private static final Logger LOG = LoggerFactory.getLogger(ConsumidorRabbit.class);
    private final ObjectMapper json;
    private final ProcessamentoEvento processamento;
    private final PublicacaoRabbit publicacao;

    ConsumidorRabbit(ObjectMapper json, ProcessamentoEvento processamento, PublicacaoRabbit publicacao) {
        this.json = json;
        this.processamento = processamento;
        this.publicacao = publicacao;
    }

    @RabbitListener(queues = TopologiaEventos.TRABALHO, ackMode = "MANUAL")
    void receber(Message mensagem, Channel canal) throws IOException {
        long entrega = mensagem.getMessageProperties().getDeliveryTag();
        try {
            Evento evento = json.readValue(mensagem.getBody(), Evento.class);
            processamento.processar(evento);
            publicacao.confirmar(TopologiaEventos.NOTIFICACOES, "", publicacao.mensagem(evento));
        } catch (Exception erro) {
            reagendar(mensagem, canal, entrega, erro);
            return;
        }
        canal.basicAck(entrega, false);
    }

    private void reagendar(Message mensagem, Channel canal, long entrega, Exception erro) throws IOException {
        var tentativa = mensagem.getMessageProperties().<Integer>getHeader(TopologiaEventos.TENTATIVA);
        int atual = tentativa == null ? 0 : tentativa;
        LOG.warn("Falha no evento {}, tentativa {}: {}", mensagem.getMessageProperties().getMessageId(),
                atual + 1, erro.getMessage());
        if (atual >= 2) {
            canal.basicNack(entrega, false, false);
            return;
        }
        var copia = MessageBuilder.fromClonedMessage(mensagem)
                .setHeader(TopologiaEventos.TENTATIVA, atual + 1).build();
        try {
            publicacao.confirmar("", TopologiaEventos.RETRY + (atual + 1), copia);
        } catch (Exception erroPublicacao) {
            canal.basicNack(entrega, false, true);
            return;
        }
        canal.basicAck(entrega, false);
    }
}
