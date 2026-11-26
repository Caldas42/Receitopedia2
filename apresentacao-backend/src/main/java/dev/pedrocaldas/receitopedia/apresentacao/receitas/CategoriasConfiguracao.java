package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.pedrocaldas.receitopedia.aplicacao.receitas.CategoriaRepositorio;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.CategoriaServicoAplicacao;

@Configuration
class CategoriasConfiguracao {
    @Bean
    CategoriaServicoAplicacao categorias(CategoriaRepositorio repositorio) {
        return new CategoriaServicoAplicacao(repositorio);
    }
}
