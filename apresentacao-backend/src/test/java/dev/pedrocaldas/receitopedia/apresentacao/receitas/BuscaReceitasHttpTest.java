package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import static dev.pedrocaldas.receitopedia.apresentacao.ClienteReceitopedia.SENHA_TESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

import dev.pedrocaldas.receitopedia.BackendAplicacao;
import dev.pedrocaldas.receitopedia.aplicacao.usuarios.PromoverAdmin;
import dev.pedrocaldas.receitopedia.apresentacao.ClienteReceitopedia;

@SpringBootTest(classes = BackendAplicacao.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=${BUSCA_TEST_DB_URL:jdbc:h2:mem:busca;MODE=PostgreSQL}",
                "spring.datasource.username=${BUSCA_TEST_DB_USER:sa}",
                "spring.datasource.password=${BUSCA_TEST_DB_PASSWORD:}"
        })
class BuscaReceitasHttpTest {
    private static final String RECEITAS = "/backend/receitas";

    @LocalServerPort
    private int porta;

    @Autowired
    private PromoverAdmin promover;

    private ClienteReceitopedia dono;

    @BeforeEach
    void abrirSessao() {
        dono = novoDono();
    }

    @Test
    void buscaAutorPorTrechoLiteralECombinaComTitulo() {
        String autor = "Bruno_" + java.util.UUID.randomUUID().toString().substring(0, 7);
        String email = autor.toLowerCase(Locale.ROOT) + "@example.com";
        assertThat(dono.cadastrar(email, autor, SENHA_TESTE).getStatusCode()).isEqualTo(CREATED);
        assertThat(dono.entrar(email, SENHA_TESTE).getStatusCode()).isEqualTo(NO_CONTENT);

        String principal = criarReceita(dono, "Canjica de coco");
        String outra = criarReceita(dono, "Mungunzá");
        criarReceita(novoDono(), "Canjica salgada");
        String buscaAutor = "BRÚNO_" + autor.substring(6);
        var resultado = buscar(dono, RECEITAS, buscaAutor, 0);
        assertThat(ids(resultado)).containsExactlyInAnyOrder(principal, outra);
        assertThat(resultado.getBody()).allSatisfy(item -> assertThat(item.path("autor").asText()).isEqualTo(autor));
        assertThat(ids(buscar(dono, RECEITAS, buscaAutor + " canjica", 0))).containsExactly(principal);
        assertThat(ids(buscar(dono, RECEITAS, buscaAutor + "%", 0))).isEmpty();
    }

    @Test
    void procuraTrechosSemDiferenciarCaseOuAcentosEExigeCadaPalavra() {
        var admin = novoDono();
        String email = admin.ler("/backend/sessao").getBody().path("nomeUsuario").asText()
                .toLowerCase(Locale.ROOT) + "@example.com";
        assertThat(promover.executar(email)).isTrue();
        assertThat(admin.entrar(email, SENHA_TESTE).getStatusCode()).isEqualTo(NO_CONTENT);
        String queijos = criarTag(admin, "Queijo Curado");
        String doces = criarTag(admin, "Pães Doces");

        String titulo = criarReceita(dono, "Pão de Milho", queijos);
        // Título com acento decomposto, como após colar texto de outra fonte.
        String decomposta = criarReceita(dono, "Pa\u0303o Integral");
        String tag = criarReceita(dono, "Assado da Casa", doces);
        String duasTags = criarReceita(dono, "Torta Caseira", doces, queijos);
        criarReceita(dono, "Arroz");

        assertThat(ids(buscar(dono, RECEITAS, "  pAo  ", 0))).containsExactlyInAnyOrder(titulo, decomposta);
        assertThat(ids(buscar(dono, RECEITAS, "quei", 0))).containsExactlyInAnyOrder(titulo, duasTags);
        assertThat(ids(buscar(dono, RECEITAS, "PAO  cur", 0))).containsExactly(titulo);
        assertThat(ids(buscar(dono, RECEITAS, "PAE doc", 0))).containsExactlyInAnyOrder(tag, duasTags);
        assertThat(ids(buscar(dono, RECEITAS, "doc cur", 0))).containsExactly(duasTags);
        assertThat(ids(buscar(dono, RECEITAS, "pao sopa", 0))).isEmpty();
        String coreano = criarReceita(dono, "한".repeat(120));
        assertThat(ids(buscar(dono, RECEITAS, "한", 0))).containsExactly(coreano);
    }

    @Test
    void trataCuringasComoTextoLiteralEValidaOsParametros() {
        String autor = "Literal" + java.util.UUID.randomUUID().toString().substring(0, 7);
        String email = autor.toLowerCase(Locale.ROOT) + "@example.com";
        assertThat(dono.cadastrar(email, autor, SENHA_TESTE).getStatusCode()).isEqualTo(CREATED);
        assertThat(dono.entrar(email, SENHA_TESTE).getStatusCode()).isEqualTo(NO_CONTENT);
        String literal = criarReceita(dono, "Especial 100% _ !");
        criarReceita(dono, "Especial 100X A");
        assertThat(ids(buscar(dono, RECEITAS, "100%", 0))).containsExactly(literal);
        assertThat(ids(buscar(dono, RECEITAS, "especial _", 0))).containsExactly(literal);
        assertThat(ids(buscar(dono, RECEITAS, "!", 0))).containsExactly(literal);
        assertThat(ids(buscar(dono, RECEITAS, "%' OR 1=1", 0))).isEmpty();

        var comprida = buscar(dono, RECEITAS, "a".repeat(101), 0);
        assertThat(comprida.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(comprida.getBody().path("detail").asText()).contains("100 caracteres");
        assertThat(buscar(dono, RECEITAS, "", -1).getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    void paginaPreservandoOrdemEFiltraSomenteReceitasDoDono() {
        var proprias = new ArrayList<String>();
        for (int i = 12; i >= 0; i--) {
            proprias.add(0, criarReceita(dono, "Páginas AA %02d".formatted(i)));
        }
        String deOutro = criarReceita(novoDono(), "Páginas ZZ");
        var minhas = RECEITAS + "/minhas";

        var primeira = buscar(dono, minhas, "pagina", 0);
        var segunda = buscar(dono, minhas, "pagina", 1);
        assertThat(ids(primeira)).containsExactlyElementsOf(proprias.subList(0, 12));
        assertThat(primeira.getHeaders().getFirst("X-Tem-Proxima-Pagina")).isEqualTo("true");
        assertThat(ids(segunda)).containsExactly(proprias.get(12));
        assertThat(segunda.getHeaders().getFirst("X-Tem-Proxima-Pagina")).isEqualTo("false");

        var anonimo = new ClienteReceitopedia(porta);
        assertThat(ids(buscar(anonimo, RECEITAS, "pagina", 1))).containsExactly(proprias.get(12), deOutro);
        assertThat(ids(anonimo.ler(RECEITAS))).containsExactlyElementsOf(ids(buscar(anonimo, RECEITAS, "   ", 0)));
    }

    private String criarTag(ClienteReceitopedia admin, String nome) {
        var resposta = admin.escrever("/backend/categorias", POST, Map.of("nome", nome));
        assertThat(resposta.getStatusCode()).isEqualTo(CREATED);
        return resposta.getBody().path("id").asText();
    }

    private String criarReceita(ClienteReceitopedia cliente, String titulo, String... categorias) {
        var resposta = cliente.escrever(RECEITAS, POST, Map.of("titulo", titulo, "ingredientes", List.of("Água"),
                "etapas", List.of("Misture."), "categorias", List.of(categorias)));
        assertThat(resposta.getStatusCode()).isEqualTo(CREATED);
        return resposta.getBody().path("id").asText();
    }

    private ClienteReceitopedia novoDono() {
        var cliente = new ClienteReceitopedia(porta);
        cliente.criarContaESessao();
        return cliente;
    }

    private ResponseEntity<JsonNode> buscar(ClienteReceitopedia cliente, String caminho, String termo, int pagina) {
        return cliente.ler(caminho + "?busca={termo}&pagina={pagina}", termo, pagina);
    }

    private List<String> ids(ResponseEntity<JsonNode> resposta) {
        assertThat(resposta.getStatusCode()).isEqualTo(OK);
        var ids = new ArrayList<String>();
        resposta.getBody().forEach(receita -> ids.add(receita.path("id").asText()));
        return ids;
    }
}
