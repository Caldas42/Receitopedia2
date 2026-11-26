package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.pedrocaldas.receitopedia.aplicacao.receitas.CatalogoAlterado;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.CategoriaDuplicada;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.CategoriaNaoEncontrada;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.CategoriaServicoAplicacao;
import dev.pedrocaldas.receitopedia.dominio.receitas.Categoria;

@RestController
@RequestMapping("/backend/categorias")
class CategoriaControlador {
    private final CategoriaServicoAplicacao servico;

    CategoriaControlador(CategoriaServicoAplicacao servico) {
        this.servico = servico;
    }

    @GetMapping
    List<Categoria> listar() {
        return servico.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Categoria adicionar(@RequestBody Formulario formulario) {
        return servico.adicionar(formulario.nome());
    }

    @PutMapping("/{id}")
    Categoria editar(@PathVariable UUID id, @RequestBody Formulario formulario) {
        return servico.editar(id, formulario.nome());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remover(@PathVariable UUID id) {
        servico.remover(id);
    }

    @PutMapping("/ordem")
    List<Categoria> reordenar(@RequestBody Ordem ordem) {
        return servico.reordenar(ordem.ids());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail nomeInvalido(IllegalArgumentException erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, erro.getMessage());
    }

    @ExceptionHandler(CategoriaNaoEncontrada.class)
    ProblemDetail naoEncontrada(CategoriaNaoEncontrada erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
    }

    @ExceptionHandler({CategoriaDuplicada.class, CatalogoAlterado.class})
    ProblemDetail conflito(RuntimeException erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, erro.getMessage());
    }

    record Formulario(String nome) {
    }

    record Ordem(List<UUID> ids) {
    }
}
