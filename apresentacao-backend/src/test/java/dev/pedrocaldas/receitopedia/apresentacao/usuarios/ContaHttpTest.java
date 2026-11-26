package dev.pedrocaldas.receitopedia.apresentacao.usuarios;

import static dev.pedrocaldas.receitopedia.apresentacao.ClienteReceitopedia.SENHA_TESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.*;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import dev.pedrocaldas.receitopedia.BackendAplicacao;
import dev.pedrocaldas.receitopedia.apresentacao.ClienteReceitopedia;

@SpringBootTest(classes = BackendAplicacao.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=${RECEITAS_TEST_DB_URL:jdbc:h2:mem:receitas-bdd;MODE=PostgreSQL}",
                "spring.datasource.username=${RECEITAS_TEST_DB_USER:sa}",
                "spring.datasource.password=${RECEITAS_TEST_DB_PASSWORD:}"
        })
class ContaHttpTest {
    @LocalServerPort
    private int porta;

    @Test
    void cadastraAutenticaEEncerraSessaoComCsrf() {
        var navegador = new ClienteReceitopedia(porta);
        String nome = navegador.criarContaESessao();
        assertThat(navegador.ler("/backend/sessao").getBody().path("nomeUsuario").asText()).isEqualTo(nome);
        assertThat(navegador.escreverSemCsrf("/backend/logout", null).getStatusCode()).isEqualTo(FORBIDDEN);
        assertThat(navegador.escrever("/backend/logout", POST, null).getStatusCode()).isEqualTo(NO_CONTENT);
        assertThat(navegador.ler("/backend/sessao").getStatusCode()).isEqualTo(UNAUTHORIZED);
        assertThat(navegador.entrar(" " + nome.toUpperCase(java.util.Locale.ROOT) + "@example.com ", SENHA_TESTE)
                .getStatusCode()).isEqualTo(NO_CONTENT);
        var desconhecido = new ClienteReceitopedia(porta);
        var emailInvalido = desconhecido.entrar("inexistente@example.com", "senha errada");
        var senhaInvalida = desconhecido.entrar(nome + "@example.com", "senha errada");
        assertThat(emailInvalido.getStatusCode()).isEqualTo(UNAUTHORIZED);
        assertThat(senhaInvalida.getBody()).isEqualTo(emailInvalido.getBody());
    }

    @Test
    void rejeitaNomeDuplicadoSemDiferenciarCaixa() {
        var navegador = new ClienteReceitopedia(porta);
        String nome = navegador.criarContaESessao();
        var repetido = navegador.cadastrar("outro-" + nome + "@example.com",
                nome.toUpperCase(java.util.Locale.ROOT), SENHA_TESTE);
        assertThat(repetido.getStatusCode()).isEqualTo(CONFLICT);
        assertThat(repetido.getBody().path("campos").has("nomeUsuario")).isTrue();
    }

    @Test
    void aceitaApenasUmCadastroSimultaneoDoMesmoEmail() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 12);
        var inicio = new CountDownLatch(1);
        var grupo = Executors.newFixedThreadPool(2);
        try {
            var a = grupo.submit(() -> {
                inicio.await();
                return new ClienteReceitopedia(porta).cadastrar(sufixo + "@EXAMPLE.COM", "a" + sufixo, SENHA_TESTE);
            });
            var b = grupo.submit(() -> {
                inicio.await();
                return new ClienteReceitopedia(porta).cadastrar(" " + sufixo + "@example.com ", "b" + sufixo, SENHA_TESTE);
            });
            inicio.countDown();
            var primeiro = a.get(15, TimeUnit.SECONDS);
            var segundo = b.get(15, TimeUnit.SECONDS);
            assertThat(primeiro.getStatusCode()).isNotEqualTo(segundo.getStatusCode());
            var duplicado = primeiro.getStatusCode() == CREATED ? segundo : primeiro;
            assertThat(duplicado.getStatusCode()).isEqualTo(CONFLICT);
            assertThat(duplicado.getBody().path("campos").has("email")).isTrue();
        } finally {
            grupo.shutdownNow();
        }
    }
}
