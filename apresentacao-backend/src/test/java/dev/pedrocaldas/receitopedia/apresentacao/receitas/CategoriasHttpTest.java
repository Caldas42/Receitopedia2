package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import static dev.pedrocaldas.receitopedia.apresentacao.ClienteReceitopedia.SENHA_TESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

import dev.pedrocaldas.receitopedia.BackendAplicacao;
import dev.pedrocaldas.receitopedia.aplicacao.usuarios.PromoverAdmin;
import dev.pedrocaldas.receitopedia.apresentacao.ClienteReceitopedia;

@SpringBootTest(classes = BackendAplicacao.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=${CATEGORIAS_TEST_DB_URL:jdbc:h2:mem:categorias;MODE=PostgreSQL}",
                "spring.datasource.username=${CATEGORIAS_TEST_DB_USER:sa}",
                "spring.datasource.password=${CATEGORIAS_TEST_DB_PASSWORD:}",
                "RECEITOPEDIA_ADMIN_EMAIL= Admin.Configurado@example.com "
        })
class CategoriasHttpTest {
    private static final String CATEGORIAS = "/backend/categorias";

    @LocalServerPort
    private int porta;

    @Autowired
    private PromoverAdmin promover;

    @Autowired
    @Qualifier("promoverAdminConfigurado")
    private ApplicationRunner bootstrap;

    @Test
    void exigeAdminEPermiteConsultaPublica() throws Exception {
        var anonimo = new ClienteReceitopedia(porta);
        assertThat(adicionar(anonimo, "Sopas").getStatusCode()).isEqualTo(UNAUTHORIZED);
        assertThat(anonimo.ler(CATEGORIAS).getStatusCode()).isEqualTo(OK);

        var usuario = new ClienteReceitopedia(porta);
        assertThat(usuario.cadastrar("admin.configurado@example.com", "ConfigAdmin", SENHA_TESTE)
                .getStatusCode()).isEqualTo(CREATED);
        assertThat(usuario.entrar("admin.configurado@example.com", SENHA_TESTE).getStatusCode()).isEqualTo(NO_CONTENT);
        assertThat(usuario.ler("/backend/sessao").getBody().path("papel").asText()).isEqualTo("USER");
        assertThat(adicionar(usuario, "Sopas").getStatusCode()).isEqualTo(FORBIDDEN);

        bootstrap.run(new DefaultApplicationArguments());
        assertThat(usuario.entrar("admin.configurado@example.com", SENHA_TESTE).getStatusCode()).isEqualTo(NO_CONTENT);
        assertThat(usuario.ler("/backend/sessao").getBody().path("papel").asText()).isEqualTo("ADMIN");
        assertThat(adicionar(usuario, "Sopas").getStatusCode()).isEqualTo(CREATED);
        assertThat(anonimo.ler(CATEGORIAS).getBody()).anySatisfy(categoria ->
                assertThat(categoria.path("nome").asText()).isEqualTo("Sopas"));
    }

    @Test
    void preservaGrafiaNormalizaEspacosERejeitaNomesInvalidosOuEquivalentes() {
        var admin = entrarComoAdmin();
        var criada = adicionar(admin, "  PÃO   com   Açúcar ");
        assertThat(criada.getStatusCode()).isEqualTo(CREATED);
        assertThat(criada.getBody().path("nome").asText()).isEqualTo("PÃO com Açúcar");

        var duplicada = adicionar(admin, "pao com acucar");
        assertThat(duplicada.getStatusCode()).isEqualTo(CONFLICT);
        assertThat(duplicada.getBody().path("detail").asText()).contains("Já existe uma categoria");

        for (String invalido : new String[]{" A  ", "x".repeat(31)}) {
            var resposta = adicionar(admin, invalido);
            assertThat(resposta.getStatusCode()).isEqualTo(BAD_REQUEST);
            assertThat(resposta.getBody().path("detail").asText()).contains("2 e 30 caracteres");
        }
        assertThat(adicionar(admin, "x".repeat(30)).getStatusCode()).isEqualTo(CREATED);
    }

    @Test
    void aceitaUmaUnicaCriacaoSimultaneaDoMesmoNome() throws Exception {
        var admin = entrarComoAdmin();
        admin.ler("/backend/csrf");
        var inicio = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var uma = executor.submit(() -> {
                inicio.await();
                return adicionar(admin, "Chá Gelado");
            });
            var outra = executor.submit(() -> {
                inicio.await();
                return adicionar(admin, "  CHA  GELADO  ");
            });
            inicio.countDown();
            var a = uma.get(15, TimeUnit.SECONDS);
            var b = outra.get(15, TimeUnit.SECONDS);
            assertThat(java.util.List.of(a.getStatusCode(), b.getStatusCode()))
                    .containsExactlyInAnyOrder(CREATED, CONFLICT);
        } finally {
            executor.shutdownNow();
        }
    }

    private ClienteReceitopedia entrarComoAdmin() {
        var cliente = new ClienteReceitopedia(porta);
        String email = (cliente.criarContaESessao() + "@example.com").toLowerCase(Locale.ROOT);
        assertThat(promover.executar(email)).isTrue();
        assertThat(cliente.entrar(email, SENHA_TESTE).getStatusCode()).isEqualTo(NO_CONTENT);
        return cliente;
    }

    private ResponseEntity<JsonNode> adicionar(ClienteReceitopedia cliente, String nome) {
        return cliente.escrever(CATEGORIAS, POST, Map.of("nome", nome));
    }
}
