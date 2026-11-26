package dev.pedrocaldas.receitopedia.apresentacao.usuarios;

import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.pedrocaldas.receitopedia.aplicacao.usuarios.CadastrarUsuario;
import dev.pedrocaldas.receitopedia.aplicacao.usuarios.CadastroDuplicado;
import dev.pedrocaldas.receitopedia.dominio.usuarios.CadastroInvalido;
import dev.pedrocaldas.receitopedia.dominio.usuarios.Papel;

@RestController
@ConditionalOnWebApplication
@RequestMapping("/backend")
class ContaControlador {
    private final CadastrarUsuario cadastro;

    ContaControlador(CadastrarUsuario cadastro) {
        this.cadastro = cadastro;
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    void cadastrar(@RequestBody Formulario formulario) {
        cadastro.cadastrar(formulario.email(), formulario.nomeUsuario(), formulario.senha());
    }

    @GetMapping("/sessao")
    Sessao sessao(@AuthenticationPrincipal UsuarioPrincipal pessoa) {
        return new Sessao(pessoa.id(), pessoa.nomeUsuario(), pessoa.papel());
    }

    @GetMapping("/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @ExceptionHandler(CadastroInvalido.class)
    ProblemDetail cadastroInvalido(CadastroInvalido erro) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, erro.getMessage());
        problema.setProperty("campos", erro.erros());
        return problema;
    }

    @ExceptionHandler(CadastroDuplicado.class)
    ProblemDetail cadastroDuplicado(CadastroDuplicado erro) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Confira os campos do cadastro.");
        problema.setProperty("campos", Map.of(erro.campo(), erro.getMessage()));
        return problema;
    }

    record Formulario(String email, String nomeUsuario, String senha) {
        @Override
        public String toString() {
            return "Formulario";
        }
    }

    record Sessao(UUID id, String nomeUsuario, Papel papel) {
    }
}
