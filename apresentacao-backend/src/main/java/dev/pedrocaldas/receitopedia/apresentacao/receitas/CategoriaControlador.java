package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.pedrocaldas.receitopedia.aplicacao.receitas.CategoriaDuplicada;
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

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail nomeInvalido(IllegalArgumentException erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, erro.getMessage());
    }

    @ExceptionHandler(CategoriaDuplicada.class)
    ProblemDetail nomeDuplicado(CategoriaDuplicada erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, erro.getMessage());
    }

    record Formulario(String nome) {
    }
}
