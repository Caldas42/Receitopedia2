package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static dev.pedrocaldas.receitopedia.apresentacao.ClienteReceitopedia.SENHA_TESTE;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

import dev.pedrocaldas.receitopedia.apresentacao.ClienteReceitopedia;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;

public class ReceitasPassos {
    private static final String RECEITAS = "/backend/receitas";
    private static final List<String> INGREDIENTES = List.of("2 cenouras", "3 ovos");
    private static final List<String> ETAPAS = List.of("Bata os ingredientes.", "Leve ao forno.");

    @LocalServerPort
    private int porta;
    @Autowired
    private JdbcTemplate jdbc;
    private ClienteReceitopedia http;
    private String nomeDono;

    @Before
    public void iniciarCliente() {
        http = new ClienteReceitopedia(porta);
        nomeDono = http.criarContaESessao();
    }

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
        var criada = http.escrever(RECEITAS, POST, formulario(titulo, INGREDIENTES));
        assertThat(criada.getStatusCode()).isEqualTo(CREATED);
        var receita = criada.getBody();
        id = receita.path("id").asText();
        assertThat(criada.getHeaders().getLocation()).hasToString(caminhoReceita());
        versao = versaoOriginal = receita.path("versao").asLong();
    }

    @Então("a receita aparece na lista")
    public void apareceNaLista() {
        var lista = http.ler(RECEITAS);
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
        var exclusao = http.escrever(caminhoReceita() + "?versao=" + versao, DELETE, null);
        assertThat(exclusao.getStatusCode()).isEqualTo(NO_CONTENT);
    }

    @Então("a receita deixa de aparecer na lista e o detalhe retorna 404")
    public void receitaRemovida() {
        var lista = http.ler(RECEITAS);
        assertThat(lista.getBody()).noneSatisfy(receita -> assertThat(receita.path("id").asText()).isEqualTo(id));
        assertThat(http.ler(caminhoReceita()).getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Quando("tento cadastrar {string} sem ingredientes")
    public void cadastrarSemIngredientes(String titulo) {
        resposta = http.escrever(RECEITAS, POST, formulario(titulo, List.of()));
    }

    @Então("o cadastro é recusado com a mensagem {string}")
    public void cadastroRecusado(String mensagem) {
        assertThat(resposta.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(resposta.getBody().path("detail").asText()).isEqualTo(mensagem);
    }

    @Quando("dois clientes alteram somente os ingredientes usando a mesma versão")
    public void edicoesSimultaneas() throws Exception {
        http.ler("/backend/csrf");
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
        var exclusao = http.escrever(caminhoReceita() + "?versao=" + versaoOriginal, DELETE, null);
        assertThat(exclusao.getStatusCode()).isEqualTo(CONFLICT);
        consultar();
    }

    @Então("a receita é pública e identifica seu dono")
    public void conferirAutoriaPublica() {
        var anonimo = new ClienteReceitopedia(porta);
        var receita = anonimo.ler(caminhoReceita());
        assertThat(receita.getStatusCode()).isEqualTo(OK);
        assertThat(receita.getBody().path("dono").path("nomeUsuario").asText()).isEqualTo(nomeDono);
    }

    @Então("um visitante precisa entrar para criar receitas")
    public void exigirLogin() {
        var anonimo = new ClienteReceitopedia(porta);
        assertThat(anonimo.escrever(RECEITAS, POST, formulario("Outra receita", INGREDIENTES))
                .getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    @Então("outro usuário e um administrador recebem 403 ao editar ou remover a receita")
    public void autorizarApenasDono() {
        var outro = new ClienteReceitopedia(porta);
        outro.criarContaESessao();
        var admin = new ClienteReceitopedia(porta);
        var nome = admin.criarContaESessao();
        String email = (nome + "@example.com").toLowerCase(java.util.Locale.ROOT);
        jdbc.update("update usuario set papel = 'ADMIN' where email = ?", email);
        assertThat(admin.escrever("/backend/logout", POST, null).getStatusCode()).isEqualTo(NO_CONTENT);
        assertThat(admin.entrar(email, SENHA_TESTE).getStatusCode()).isEqualTo(NO_CONTENT);

        var corpo = formulario("Alteração indevida", INGREDIENTES);
        corpo.put("versao", versao);
        for (var cliente : List.of(outro, admin)) {
            assertThat(cliente.escrever(caminhoReceita(), PUT, corpo).getStatusCode()).isEqualTo(FORBIDDEN);
            assertThat(cliente.escrever(caminhoReceita() + "?versao=" + versao, DELETE, null)
                    .getStatusCode()).isEqualTo(FORBIDDEN);
        }
        consultarTitulo(titulo);
    }

    @Então("Minhas Receitas lista somente as receitas do usuário logado")
    public void listarSomenteAsMinhasReceitas() {
        var outro = new ClienteReceitopedia(porta);
        outro.criarContaESessao();
        var criadaPorOutro = outro.escrever(RECEITAS, POST, formulario("Macarrão", INGREDIENTES));
        assertThat(criadaPorOutro.getStatusCode()).isEqualTo(CREATED);
        String outroId = criadaPorOutro.getBody().path("id").asText();
        var minhas = http.ler(RECEITAS + "/minhas");
        assertThat(minhas.getStatusCode()).isEqualTo(OK);
        assertThat(minhas.getBody()).extracting(receita -> receita.path("id").asText()).containsExactly(id);

        var anonimo = new ClienteReceitopedia(porta);
        var publicas = anonimo.ler(RECEITAS);
        assertThat(publicas.getBody()).extracting(receita -> receita.path("id").asText()).contains(id, outroId);
        assertThat(anonimo.ler(RECEITAS + "/minhas").getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    @Quando("cadastro ingredientes e modo de preparo sem título")
    public void cadastrarSemTitulo() {
        var corpo = formulario("", INGREDIENTES);
        corpo.remove("titulo");
        corpo.remove("descricao");
        var criada = http.escrever(RECEITAS, POST, corpo);
        assertThat(criada.getStatusCode()).isEqualTo(CREATED);
        id = criada.getBody().path("id").asText();
    }

    @Então("o título fica vazio no detalhe da receita")
    public void tituloOpcional() {
        assertThat(consultar().path("dados").path("titulo").asText()).isEmpty();
    }

    @Quando("cadastro uma receita com {int} minutos e sem horas")
    public void cadastrarSoMinutos(int minutos) {
        var corpo = formulario("Salada", INGREDIENTES);
        corpo.put("minutos", minutos);
        var criada = http.escrever(RECEITAS, POST, corpo);
        assertThat(criada.getStatusCode()).isEqualTo(CREATED);
        id = criada.getBody().path("id").asText();
    }

    @Então("o tempo consta como {int} minutos no detalhe e na lista")
    public void conferirTempo(int minutos) {
        assertThat(consultar().path("dados").path("tempoPreparo").path("minutosTotais").asInt()).isEqualTo(minutos);
        assertThat(http.ler(RECEITAS).getBody()).anySatisfy(receita -> {
            assertThat(receita.path("id").asText()).isEqualTo(id);
            assertThat(receita.path("tempoMinutos").asInt()).isEqualTo(minutos);
        });
    }

    @Então("horas fracionárias são rejeitadas")
    public void rejeitarHorasFracionarias() {
        var corpo = formulario("Salada", INGREDIENTES);
        corpo.put("horas", 1.5);
        assertThat(http.escrever(RECEITAS, POST, corpo).getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    private JsonNode consultar() {
        var consulta = http.ler(caminhoReceita());
        assertThat(consulta.getStatusCode()).isEqualTo(OK);
        return consulta.getBody();
    }

    private ResponseEntity<JsonNode> editar(String titulo, List<String> ingredientes) {
        var corpo = formulario(titulo, ingredientes);
        corpo.put("versao", versao);
        return http.escrever(caminhoReceita(), PUT, corpo);
    }

    private String caminhoReceita() {
        return RECEITAS + "/" + id;
    }

    private Map<String, Object> formulario(String titulo, List<String> ingredientes) {
        return new LinkedHashMap<>(Map.of("titulo", titulo, "descricao", "Receita de teste",
                "ingredientes", ingredientes, "etapas", ETAPAS));
    }
}
