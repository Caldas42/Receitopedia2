package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReceitasPaginas {
    @GetMapping({"/entrar", "/cadastro", "/categorias", "/mensagens", "/minhas-receitas", "/receitas", "/receitas/{id}", "/receitas/{id}/editar"})
    public String carregarAngular() {
        return "forward:/index.html";
    }
}
