package dev.pedrocaldas.receitopedia.apresentacao.pessoas;

import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import dev.pedrocaldas.receitopedia.aplicacao.pessoas.PessoasRepositorio;
import dev.pedrocaldas.receitopedia.aplicacao.pessoas.PessoasServico;
import dev.pedrocaldas.receitopedia.apresentacao.usuarios.UsuarioPrincipal;
import dev.pedrocaldas.receitopedia.dominio.comunicacao.ConversaIndisponivel;

@RestController
@RequestMapping("/backend/pessoas")
class PessoasControlador {
    private final PessoasServico servico;

    PessoasControlador(PessoasServico servico) {
        this.servico = servico;
    }

    @Bean
    static PessoasServico pessoasServico(PessoasRepositorio repositorio) {
        return new PessoasServico(repositorio);
    }

    @GetMapping
    PessoasServico.Pagina listar(@AuthenticationPrincipal UsuarioPrincipal ator, @RequestParam PessoasRepositorio.Lista lista,
            @RequestParam(defaultValue = "") String busca, @RequestParam(defaultValue = "") String cursor) {
        return servico.buscar(ator.id(), lista, busca, cursor);
    }

    @GetMapping("/{alvo}/seguimento")
    Map<String, Boolean> segue(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID alvo) {
        return Map.of("seguindo", servico.segue(ator.id(), alvo));
    }

    @PutMapping("/{alvo}/seguimento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void seguir(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID alvo) {
        servico.alterarSeguimento(ator.id(), alvo, true);
    }

    @DeleteMapping("/{alvo}/seguimento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deixarDeSeguir(@AuthenticationPrincipal UsuarioPrincipal ator, @PathVariable UUID alvo) {
        servico.alterarSeguimento(ator.id(), alvo, false);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalido(IllegalArgumentException erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, erro.getMessage());
    }

    @ExceptionHandler(ConversaIndisponivel.class)
    ProblemDetail indisponivel() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Usuário indisponível.");
    }
}
