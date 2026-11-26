package dev.pedrocaldas.receitopedia.infraestrutura.eventos;

import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "receitopedia.eventos.reprocessar", havingValue = "true")
class ReprocessarDlq implements ApplicationRunner {
    private final RabbitTemplate rabbit;
    private final PublicacaoRabbit publicacao;
    private final ConfigurableApplicationContext contexto;

    ReprocessarDlq(RabbitTemplate rabbit, PublicacaoRabbit publicacao, ConfigurableApplicationContext contexto) {
        this.rabbit = rabbit;
        this.publicacao = publicacao;
        this.contexto = contexto;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        Integer reprocessados = rabbit.execute(canal -> {
            long disponiveis = canal.messageCount(TopologiaEventos.DLQ);
            int total = 0;
            for (long i = 0; i < disponiveis; i++) {
                var mensagem = canal.basicGet(TopologiaEventos.DLQ, false);
                if (mensagem == null) {
                    break;
                }
                long entrega = mensagem.getEnvelope().getDeliveryTag();
                try {
                    var nova = MessageBuilder.withBody(mensagem.getBody())
                            .setContentType("application/json").setMessageId(mensagem.getProps().getMessageId())
                            .setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
                    publicacao.confirmar(TopologiaEventos.EVENTOS, TopologiaEventos.CHAVE, nova);
                    canal.basicAck(entrega, false);
                    total++;
                } catch (Exception erro) {
                    canal.basicNack(entrega, false, true);
                    throw new IllegalStateException("Evento permanece na DLQ.", erro);
                }
            }
            return total;
        });
        System.out.println("Eventos reenviados da DLQ: " + reprocessados);
        System.exit(SpringApplication.exit(contexto));
    }
}
