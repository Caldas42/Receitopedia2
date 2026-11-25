package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;

public class ReceitasPassos {
    private static final String RECEITAS = "/backend/receitas";
    private static final List<String> INGREDIENTES = List.of("2 cenouras", "3 ovos");
    private static final List<String> ETAPAS = List.of("Bata os ingredientes.", "Leve ao forno.");

    @Autowired
    private TestRestTemplate http;

    private String id;
    private String titulo;
    private long versao;
    private long versaoOriginal;
    private ResponseEntity<JsonNode> resposta;
    private List<ResponseEntity<JsonNode>> edicoes;

    @Dado("que cadastrei uma receita com o título {string}")
    @Quando("cadastro uma receita com o título {string}")
    public void cadastrar(String titulo) {
        this.titulo = titulo;
        var criada = http.postForEntity(RECEITAS, formulario(titulo, INGREDIENTES), JsonNode.class);
        assertThat(criada.getStatusCode()).isEqualTo(CREATED);
        var receita = criada.getBody();
        id = receita.path("id").asText();
        assertThat(criada.getHeaders().getLocation()).hasToString(caminhoReceita());
        versao = versaoOriginal = receita.path("versao").asLong();
    }

    @Então("a receita aparece na lista")
    public void apareceNaLista() {
        var lista = http.getForEntity(RECEITAS, JsonNode.class);
        assertThat(lista.getStatusCode()).isEqualTo(OK);
        assertThat(lista.getBody()).anySatisfy(receita -> {
            assertThat(receita.path("id").asText()).isEqualTo(id);
            assertThat(receita.path("titulo").asText()).isEqualTo(titulo);
        });
    }

    @Então("o detalhe mostra os ingredientes e as etapas na ordem cadastrada")
    public void detalheExibeListasOrdenadas() {
        var dados = consultar().path("dados");
        assertThat(dados.path("ingredientes")).extracting(JsonNode::asText).containsExactlyElementsOf(INGREDIENTES);
        assertThat(dados.path("etapas")).extracting(JsonNode::asText).containsExactlyElementsOf(ETAPAS);
    }

    @Quando("edito o título para {string}")
    public void editarTitulo(String novoTitulo) {
        var edicao = editar(novoTitulo, INGREDIENTES);
        assertThat(edicao.getStatusCode()).isEqualTo(OK);
        versao = edicao.getBody().path("versao").asLong();
        assertThat(versao).isGreaterThan(versaoOriginal);
    }

    @Então("consulto a receita com o título {string}")
    public void consultarTitulo(String tituloEsperado) {
        assertThat(consultar().path("dados").path("titulo").asText()).isEqualTo(tituloEsperado);
    }

    @Quando("removo a receita na versão atual")
    public void remover() {
        var exclusao = http.exchange(caminhoReceita() + "?versao=" + versao, DELETE, null, Void.class);
        assertThat(exclusao.getStatusCode()).isEqualTo(NO_CONTENT);
    }

    @Então("a receita deixa de aparecer na lista e o detalhe retorna 404")
    public void receitaRemovida() {
        var lista = http.getForEntity(RECEITAS, JsonNode.class);
        assertThat(lista.getBody()).noneSatisfy(receita -> assertThat(receita.path("id").asText()).isEqualTo(id));
        assertThat(http.getForEntity(caminhoReceita(), JsonNode.class).getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Quando("tento cadastrar {string} sem ingredientes")
    public void cadastrarSemIngredientes(String titulo) {
        resposta = http.postForEntity(RECEITAS, formulario(titulo, List.of()), JsonNode.class);
    }

    @Então("o cadastro é recusado com a mensagem {string}")
    public void cadastroRecusado(String mensagem) {
        assertThat(resposta.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(resposta.getBody().path("detail").asText()).isEqualTo(mensagem);
    }

    @Quando("dois clientes alteram somente os ingredientes usando a mesma versão")
    public void edicoesSimultaneas() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var prontos = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);
        try {
            var primeira = executor.submit(() -> {
                prontos.countDown();
                iniciar.await();
                return editar(titulo, List.of("Polvilho doce"));
            });
            var segunda = executor.submit(() -> {
                prontos.countDown();
                iniciar.await();
                return editar(titulo, List.of("Polvilho azedo"));
            });
            assertThat(prontos.await(5, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            edicoes = List.of(primeira.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Então("apenas uma edição é aceita e a outra retorna conflito")
    public void umaEdicaoAceita() {
        assertThat(edicoes).extracting(ResponseEntity::getStatusCode).containsExactlyInAnyOrder(OK, CONFLICT);
    }

    @Então("os ingredientes da edição aceita ficam salvos")
    public void conferirEdicaoAceita() {
        var aceita = edicoes.stream().filter(edicao -> edicao.getStatusCode().equals(OK)).findFirst().orElseThrow();
        var persistida = consultar();
        assertThat(persistida.path("dados").path("ingredientes"))
                .isEqualTo(aceita.getBody().path("dados").path("ingredientes"));
        assertThat(persistida.path("versao").asLong()).isGreaterThan(versaoOriginal);
    }

    @Então("uma exclusão baseada na versão antiga é recusada")
    public void exclusaoDesatualizada() {
        var exclusao = http.exchange(caminhoReceita() + "?versao=" + versaoOriginal, DELETE, null, JsonNode.class);
        assertThat(exclusao.getStatusCode()).isEqualTo(CONFLICT);
        consultar();
    }

    private JsonNode consultar() {
        var consulta = http.getForEntity(caminhoReceita(), JsonNode.class);
        assertThat(consulta.getStatusCode()).isEqualTo(OK);
        return consulta.getBody();
    }

    private ResponseEntity<JsonNode> editar(String titulo, List<String> ingredientes) {
        var corpo = formulario(titulo, ingredientes);
        corpo.put("versao", versao);
        return http.exchange(caminhoReceita(), PUT, new HttpEntity<>(corpo), JsonNode.class);
    }

    private String caminhoReceita() {
        return RECEITAS + "/" + id;
    }

    private Map<String, Object> formulario(String titulo, List<String> ingredientes) {
        return new LinkedHashMap<>(Map.of("titulo", titulo, "descricao", "Receita de teste",
                "ingredientes", ingredientes, "etapas", ETAPAS));
    }
}
