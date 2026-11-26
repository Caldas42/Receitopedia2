package dev.pedrocaldas.receitopedia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import dev.pedrocaldas.receitopedia.aplicacao.receitas.ArmazenamentoFotos;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaRepositorio;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaServicoAplicacao;

@SpringBootApplication
public class BackendAplicacao {
    @Bean
    ReceitaServicoAplicacao receitaServicoAplicacao(ReceitaRepositorio repositorio, ArmazenamentoFotos armazenamento) {
        return new ReceitaServicoAplicacao(repositorio, armazenamento);
    }

    public static void main(String[] args) {
        SpringApplication.run(BackendAplicacao.class, args);
    }
}
