package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaDesatualizada;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaNaoEncontrada;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaResumo;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaServicoAplicacao;
import dev.pedrocaldas.receitopedia.apresentacao.usuarios.UsuarioPrincipal;
import dev.pedrocaldas.receitopedia.dominio.receitas.AutorReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.DadosReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.ReceitaAcessoNegado;
import dev.pedrocaldas.receitopedia.dominio.receitas.Receita;
import dev.pedrocaldas.receitopedia.dominio.receitas.TempoPreparo;

@RestController
@RequestMapping("/backend/receitas")
public class ReceitaControlador {
    private final ReceitaServicoAplicacao servico;

    public ReceitaControlador(ReceitaServicoAplicacao servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<Receita> adicionar(@RequestBody Formulario formulario, @AuthenticationPrincipal UsuarioPrincipal usuario) {
        Receita receita = servico.adicionar(formulario.dados(), new AutorReceita(usuario.id(), usuario.nomeUsuario()));
        return ResponseEntity.created(URI.create("/backend/receitas/" + receita.id())).body(receita);
    }

    @GetMapping
    public List<ReceitaResumo> listar() {
        return servico.listar();
    }

    @GetMapping("/minhas")
    public List<ReceitaResumo> minhas(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return servico.listarDoDono(usuario.id());
    }

    @GetMapping("/{id}")
    public Receita visualizar(@PathVariable UUID id) {
        return servico.visualizar(id);
    }

    @PutMapping("/{id}")
    public Receita editar(@PathVariable UUID id, @RequestBody Formulario formulario,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return servico.editar(id, formulario.versao(), usuario.id(), formulario.dados());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id, @RequestParam Long versao,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        servico.remover(id, versao, usuario.id());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail dadosInvalidos(IllegalArgumentException erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, erro.getMessage());
    }

    @ExceptionHandler(ReceitaNaoEncontrada.class)
    ProblemDetail receitaNaoEncontrada(ReceitaNaoEncontrada erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
    }

    @ExceptionHandler(ReceitaAcessoNegado.class)
    ProblemDetail acessoNegado(ReceitaAcessoNegado erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, erro.getMessage());
    }

    @ExceptionHandler(ReceitaDesatualizada.class)
    ProblemDetail receitaDesatualizada(ReceitaDesatualizada erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, erro.getMessage());
    }

    record Formulario(String titulo, String descricao, List<String> ingredientes, List<String> etapas,
            Long versao, Integer horas, Integer minutos) {
        DadosReceita dados() {
            return new DadosReceita(titulo, descricao, ingredientes, etapas, TempoPreparo.informado(horas, minutos));
        }
    }
}
