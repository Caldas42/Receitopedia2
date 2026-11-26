package dev.pedrocaldas.receitopedia.apresentacao.comunicacao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.ChatServico;
import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.ConversasRepositorio;
import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat;
import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.Conversa;
import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.Mensagem;
import dev.pedrocaldas.receitopedia.apresentacao.usuarios.UsuarioPrincipal;
import dev.pedrocaldas.receitopedia.dominio.comunicacao.ConflitoMensagem;
import dev.pedrocaldas.receitopedia.dominio.comunicacao.ConversaIndisponivel;
import dev.pedrocaldas.receitopedia.dominio.comunicacao.OperacaoProibida;

@RestController
@RequestMapping("/backend/chat")
class ChatControlador {
    private final ChatServico chat;

    ChatControlador(ChatServico chat) {
        this.chat = chat;
    }

    @Bean
    static ChatServico chatServico(ConversasRepositorio repositorio) {
        return new ChatServico(repositorio);
    }

    @GetMapping("/conversas")
    List<Conversa> conversas(@AuthenticationPrincipal UsuarioPrincipal ator) {
        return chat.listar(ator.id());
    }

    @PostMapping("/conversas/dm")
    Map<String, UUID> abrir(@AuthenticationPrincipal UsuarioPrincipal ator, @RequestBody AbrirDm dados) {
        return Map.of("id", chat.abrirDm(ator.id(), dados.usuario()));
    }

    @GetMapping("/conversas/{conversa}/mensagens")
    List<Mensagem> historico(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID conversa,
            @RequestParam(defaultValue = "") String busca, @RequestParam(defaultValue = "9223372036854775807") long antes) {
        return chat.historico(ator.id(), conversa, busca, antes);
    }

    @PostMapping(value = "/conversas/{conversa}/mensagens", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mensagem enviar(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID conversa, @RequestBody Envio dados) {
        return chat.enviar(ator.id(), conversa, dados.id(), dados.texto(), List.of());
    }

    @PostMapping(value = "/conversas/{conversa}/mensagens", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mensagem enviarArquivos(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID conversa,
            @RequestPart("mensagem") Envio dados,
            @RequestPart(value = "anexos", required = false) List<MultipartFile> arquivos) throws IOException {
        var bytes = new ArrayList<byte[]>();
        if (arquivos != null) {
            if (arquivos.size() > 4) {
                throw new IllegalArgumentException("Envie até quatro imagens por mensagem.");
            }
            for (var arquivo : arquivos) {
                bytes.add(arquivo.getBytes());
            }
        }
        return chat.enviar(ator.id(), conversa, dados.id(), dados.texto(), bytes);
    }

    @PostMapping("/conversas/{conversa}/leituras")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void ler(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID conversa, @RequestBody Leituras dados) {
        chat.ler(ator.id(), conversa, dados.ids());
    }

    @PutMapping("/bloqueios/{alvo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void bloquear(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID alvo) {
        chat.bloquear(ator.id(), alvo, true);
    }

    @DeleteMapping("/bloqueios/{alvo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void desbloquear(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID alvo) {
        chat.bloquear(ator.id(), alvo, false);
    }

    @PostMapping("/conversas/grupos")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> criarGrupo(@AuthenticationPrincipal UsuarioPrincipal ator, @RequestBody CriacaoGrupo dados) {
        return Map.of("id", chat.criarGrupo(ator.id(), dados.nome(), dados.usuarios()));
    }

    @GetMapping("/conversas/{conversa}/grupo")
    DadosChat.Grupo grupo(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID conversa) {
        return chat.grupo(ator.id(), conversa);
    }

    @PutMapping("/conversas/{conversa}/grupo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void renomear(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID conversa, @RequestBody CriacaoGrupo dados) {
        chat.renomearGrupo(ator.id(), conversa, dados.nome());
    }

    @PostMapping("/conversas/{conversa}/grupo/membros")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void adicionarMembro(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID conversa, @RequestBody AbrirDm dados) {
        chat.membro(ator.id(), conversa, DadosChat.AcaoGrupo.ADICIONAR, dados.usuario(), null);
    }

    @PutMapping("/conversas/{conversa}/grupo/membros/{alvo}/admin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void promoverMembro(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID conversa, @PathVariable UUID alvo) {
        chat.membro(ator.id(), conversa, DadosChat.AcaoGrupo.PROMOVER, null, alvo);
    }

    @DeleteMapping("/conversas/{conversa}/grupo/membros/{alvo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removerMembro(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID conversa, @PathVariable UUID alvo) {
        chat.membro(ator.id(), conversa, DadosChat.AcaoGrupo.REMOVER, null, alvo);
    }

    @DeleteMapping("/conversas/{conversa}/grupo/eu")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sair(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID conversa) {
        chat.membro(ator.id(), conversa, DadosChat.AcaoGrupo.SAIR, null, null);
    }

    @PutMapping("/conversas/{conversa}/mensagens/{mensagem}")
    Mensagem editar(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID conversa,
            @PathVariable UUID mensagem, @RequestBody Edicao dados) {
        return chat.alterar(ator.id(), conversa, mensagem, dados.versao(), dados.texto(), false);
    }

    @DeleteMapping("/conversas/{conversa}/mensagens/{mensagem}")
    Mensagem apagar(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID conversa,
            @PathVariable UUID mensagem, @RequestParam Long versao) {
        return chat.alterar(ator.id(), conversa, mensagem, versao, null, true);
    }

    @GetMapping("/conversas/{conversa}/anexos/{anexo}")
    ResponseEntity<byte[]> anexo(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID conversa,
            @PathVariable UUID anexo, @RequestParam(defaultValue = "false") boolean miniatura) {
        var arquivo = chat.anexo(ator.id(), conversa, anexo, miniatura);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(arquivo.mime())).body(arquivo.bytes());
    }

    @ExceptionHandler(ConversaIndisponivel.class)
    ProblemDetail indisponivel(ConversaIndisponivel erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
    }

    @ExceptionHandler(ConflitoMensagem.class)
    ProblemDetail conflito(ConflitoMensagem erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, erro.getMessage());
    }

    @ExceptionHandler(OperacaoProibida.class)
    ProblemDetail proibido(OperacaoProibida erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, erro.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalido(IllegalArgumentException erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, erro.getMessage());
    }

    record CriacaoGrupo(String nome, List<String> usuarios) {}
    record Edicao(Long versao, String texto) {}
    record AbrirDm(String usuario) {}
    record Envio(UUID id, String texto) {}
    record Leituras(List<UUID> ids) {}
}
