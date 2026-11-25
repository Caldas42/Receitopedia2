package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import org.springframework.boot.test.context.SpringBootTest;

import dev.pedrocaldas.receitopedia.BackendAplicacao;
import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest(classes = BackendAplicacao.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=${RECEITAS_TEST_DB_URL:jdbc:h2:mem:receitas-bdd;MODE=PostgreSQL}",
                "spring.datasource.username=${RECEITAS_TEST_DB_USER:sa}",
                "spring.datasource.password=${RECEITAS_TEST_DB_PASSWORD:}"
        })
public class ReceitasCucumberConfiguracao {
}
