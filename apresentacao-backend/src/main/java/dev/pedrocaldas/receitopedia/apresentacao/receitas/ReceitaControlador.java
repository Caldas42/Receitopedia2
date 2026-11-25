package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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
import dev.pedrocaldas.receitopedia.dominio.receitas.DadosReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.Receita;

@RestController
@RequestMapping("/backend/receitas")
public class ReceitaControlador {
    private final ReceitaServicoAplicacao servico;

    public ReceitaControlador(ReceitaServicoAplicacao servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<Receita> adicionar(@RequestBody Formulario formulario) {
        Receita receita = servico.adicionar(formulario.dados());
        return ResponseEntity.created(URI.create("/backend/receitas/" + receita.id())).body(receita);
    }

    @GetMapping
    public List<ReceitaResumo> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    public Receita visualizar(@PathVariable UUID id) {
        return servico.visualizar(id);
    }

    @PutMapping("/{id}")
    public Receita editar(@PathVariable UUID id, @RequestBody Formulario formulario) {
        return servico.editar(id, formulario.versao(), formulario.dados());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id, @RequestParam Long versao) {
        servico.remover(id, versao);
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

    @ExceptionHandler(ReceitaDesatualizada.class)
    ProblemDetail receitaDesatualizada(ReceitaDesatualizada erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, erro.getMessage());
    }

    record Formulario(String titulo, String descricao, List<String> ingredientes, List<String> etapas,
            Long versao) {
        DadosReceita dados() {
            return new DadosReceita(titulo, descricao, ingredientes, etapas);
        }
    }
}
