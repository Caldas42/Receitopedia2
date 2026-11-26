package dev.pedrocaldas.receitopedia.infraestrutura.eventos;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "receitopedia.eventos.ativo", havingValue = "true")
class TopologiaEventos {
    static final String EVENTOS = "receitopedia.eventos";
    static final String CHAVE = "processar";
    static final String TRABALHO = "receitopedia.eventos.trabalho";
    static final String DLQ = "receitopedia.eventos.dlq";
    static final String RETRY = "receitopedia.eventos.retry.";
    static final String NOTIFICACOES = "receitopedia.notificacoes";
    static final String TENTATIVA = "receitopedia-tentativa";

    @Bean
    Declarables filasEventos() {
        var eventos = new DirectExchange(EVENTOS);
        var trabalho = QueueBuilder.durable(TRABALHO).deadLetterExchange("").deadLetterRoutingKey(DLQ).build();
        var retry1 = retry(1, 1000);
        var retry2 = retry(2, 5000);
        return new Declarables(eventos, trabalho, BindingBuilder.bind(trabalho).to(eventos).with(CHAVE),
                retry1, retry2, QueueBuilder.durable(DLQ).build(), new FanoutExchange(NOTIFICACOES));
    }

    private static org.springframework.amqp.core.Queue retry(int tentativa, int atraso) {
        return QueueBuilder.durable(RETRY + tentativa).ttl(atraso)
                .deadLetterExchange(EVENTOS).deadLetterRoutingKey(CHAVE).build();
    }
}
