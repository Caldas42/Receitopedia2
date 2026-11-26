package dev.pedrocaldas.receitopedia.apresentacao.comunicacao;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.ConversasRepositorio;
import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat;
import dev.pedrocaldas.receitopedia.aplicacao.eventos.Evento;
import dev.pedrocaldas.receitopedia.aplicacao.eventos.ObservadorEvento;
import dev.pedrocaldas.receitopedia.aplicacao.pessoas.PessoasServico;
import dev.pedrocaldas.receitopedia.apresentacao.usuarios.UsuarioPrincipal;

@RestController
class EventosSse implements ObservadorEvento {
    private final ConcurrentHashMap<UUID, Set<SseEmitter>> conexoes = new ConcurrentHashMap<>();
    private final ConversasRepositorio conversas;

    EventosSse(ConversasRepositorio conversas) {
        this.conversas = conversas;
    }

    @GetMapping(value = "/backend/chat/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter conectar(@AuthenticationPrincipal UsuarioPrincipal ator) {
        var emissor = new SseEmitter(55_000L);
        conexoes.compute(ator.id(), (id, existentes) -> {
            var conjunto = existentes == null ? ConcurrentHashMap.<SseEmitter>newKeySet() : existentes;
            conjunto.add(emissor);
            return conjunto;
        });
        emissor.onCompletion(() -> retirar(ator.id(), emissor));
        emissor.onTimeout(emissor::complete);
        emissor.onError(erro -> retirar(ator.id(), emissor));
        enviar(ator.id(), emissor, "conectado");
        return emissor;
    }

    @Override
    public void notificar(Evento evento) {
        if (PessoasServico.SEGUIMENTO_ALTERADO.equals(evento.tipo())) {
            notificarUsuario(evento.agregado(), "seguimento");
            notificarUsuario(evento.referencia(), "seguimento");
            return;
        }
        if (!evento.tipo().equals(DadosChat.ENVIADA) && !evento.tipo().equals(DadosChat.ALTERADA)
                && !DadosChat.eventoDm(evento.tipo())) {
            return;
        }
        var destinatarios = new HashSet<>(conversas.participantes(evento.agregado()));
        if (evento.tipo().equals(DadosChat.ALTERADA) && evento.referencia() != null) {
            destinatarios.add(evento.referencia());
        }
        destinatarios.forEach(id -> notificarUsuario(id, "atualizacao"));
    }

    private void notificarUsuario(UUID id, String tipo) {
        for (var emissor : conexoes.getOrDefault(id, Set.of())) {
            enviar(id, emissor, tipo);
        }
    }

    private void enviar(UUID id, SseEmitter emissor, String tipo) {
        try {
            emissor.send(SseEmitter.event().name(tipo).data("{}").reconnectTime(1500));
        } catch (IOException | IllegalStateException erro) {
            retirar(id, emissor);
            emissor.complete();
        }
    }

    private void retirar(UUID id, SseEmitter emissor) {
        conexoes.computeIfPresent(id, (uid, existentes) -> {
            existentes.remove(emissor);
            return existentes.isEmpty() ? null : existentes;
        });
    }
}
