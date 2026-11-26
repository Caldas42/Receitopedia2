package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import static dev.pedrocaldas.receitopedia.apresentacao.ClienteReceitopedia.SENHA_TESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
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
        assertThat(adicionar(admin, "한".repeat(30)).getStatusCode()).isEqualTo(CREATED);
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
            assertThat(List.of(a.getStatusCode(), b.getStatusCode()))
                    .containsExactlyInAnyOrder(CREATED, CONFLICT);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void associaCategoriasExistentesEPermiteRemoverOuPreservarNaEdicao() {
        var admin = entrarComoAdmin();
        var doce = idDe(adicionar(admin, "Doces para festas"));
        var forno = idDe(adicionar(admin, "Assados de forno"));
        var dono = new ClienteReceitopedia(porta);
        dono.criarContaESessao();

        var dados = dadosReceita(List.of(doce, forno));
        var criada = dono.escrever("/backend/receitas", POST, dados);
        assertThat(criada.getStatusCode()).isEqualTo(CREATED);
        String id = idDe(criada);
        String caminho = "/backend/receitas/" + id;
        conferirTags(dono.ler(caminho).getBody(), forno, doce);
        assertThat(dono.ler("/backend/receitas").getBody()).anySatisfy(resumo -> {
            assertThat(resumo.path("id").asText()).isEqualTo(id);
            conferirTags(resumo, forno, doce);
        });

        dados.put("versao", criada.getBody().path("versao").asLong());
        dados.put("categorias", List.of(doce));
        assertThat(admin.escrever(caminho, PUT, dados).getStatusCode()).isEqualTo(FORBIDDEN);
        var reduzida = dono.escrever(caminho, PUT, dados);
        assertThat(reduzida.getStatusCode()).isEqualTo(OK);
        conferirTags(reduzida.getBody(), doce);

        dados.put("versao", reduzida.getBody().path("versao").asLong());
        dados.remove("categorias");
        var preservada = dono.escrever(caminho, PUT, dados);
        assertThat(preservada.getStatusCode()).isEqualTo(OK);
        conferirTags(preservada.getBody(), doce);

        dados.put("versao", preservada.getBody().path("versao").asLong());
        dados.put("categorias", List.of());
        var removidas = dono.escrever(caminho, PUT, dados);
        assertThat(removidas.getStatusCode()).isEqualTo(OK);
        conferirTags(dono.ler(caminho).getBody());
    }

    @Test
    void limitaAsTagsERejeitaIdsRepetidosOuInexistentes() {
        var admin = entrarComoAdmin();
        var ids = java.util.stream.IntStream.range(1, 7)
                .mapToObj(numero -> idDe(adicionar(admin, "Categoria de teste " + numero))).toList();
        var dono = new ClienteReceitopedia(porta);
        dono.criarContaESessao();

        var cinco = dono.escrever("/backend/receitas", POST, dadosReceita(ids.subList(0, 5)));
        assertThat(cinco.getStatusCode()).isEqualTo(CREATED);
        assertThat(cinco.getBody().path("categorias")).hasSize(5);
        rejeitarTags(dono, ids, "até 5 categorias");
        rejeitarTags(dono, List.of(ids.get(0), ids.get(0)), "sem repetição");
        rejeitarTags(dono, List.of(UUID.randomUUID().toString()), "não existem");
    }

    @Test
    void restringeEdicaoRemocaoEOrdemAAdministradores() {
        var admin = entrarComoAdmin();
        String id = idDe(adicionar(admin, "Cadastro protegido"));
        var anonimo = new ClienteReceitopedia(porta);
        var usuario = new ClienteReceitopedia(porta);
        usuario.criarContaESessao();

        for (var cliente : List.of(anonimo, usuario)) {
            var esperado = cliente == anonimo ? UNAUTHORIZED : FORBIDDEN;
            assertThat(cliente.escrever(CATEGORIAS + "/" + id, PUT, Map.of("nome", "Outro nome"))
                    .getStatusCode()).isEqualTo(esperado);
            assertThat(cliente.escrever(CATEGORIAS + "/" + id, DELETE, null).getStatusCode()).isEqualTo(esperado);
            assertThat(cliente.escrever(CATEGORIAS + "/ordem", PUT, Map.of("ids", List.of(id)))
                    .getStatusCode()).isEqualTo(esperado);
        }
    }

    @Test
    void editaNomeEMantemReceitaAoRemoverCategoriaVinculada() {
        var admin = entrarComoAdmin();
        String id = idDe(adicionar(admin, "Para renomear"));
        String outra = idDe(adicionar(admin, "Já reservada"));
        var dono = new ClienteReceitopedia(porta);
        dono.criarContaESessao();

        var criada = dono.escrever("/backend/receitas", POST, dadosReceita(List.of(id, outra)));
        assertThat(criada.getStatusCode()).isEqualTo(CREATED);
        String receita = "/backend/receitas/" + idDe(criada);
        String categoria = CATEGORIAS + "/" + id;

        assertThat(admin.escrever(categoria, PUT, Map.of("nome", "JA RESERVADA")).getStatusCode())
                .isEqualTo(CONFLICT);
        assertThat(admin.escrever(categoria, PUT, Map.of("nome", " A ")).getStatusCode())
                .isEqualTo(BAD_REQUEST);
        var editada = admin.escrever(categoria, PUT, Map.of("nome", "  Pão   artesanal  "));
        assertThat(editada.getStatusCode()).isEqualTo(OK);
        assertThat(idDe(editada)).isEqualTo(id);
        assertThat(dono.ler(receita).getBody().path("categorias"))
                .anySatisfy(tag -> assertThat(tag.path("nome").asText()).isEqualTo("Pão artesanal"));
        assertThat(dono.ler("/backend/receitas?busca=artesanal").getBody())
                .anySatisfy(resumo -> assertThat(resumo.path("id").asText()).isEqualTo(idDe(criada)));

        assertThat(admin.escrever(categoria, DELETE, null).getStatusCode()).isEqualTo(NO_CONTENT);
        assertThat(admin.escrever(categoria, DELETE, null).getStatusCode()).isEqualTo(NOT_FOUND);
        var mantida = dono.ler(receita);
        assertThat(mantida.getStatusCode()).isEqualTo(OK);
        conferirTags(mantida.getBody(), outra);
        long versaoAntiga = criada.getBody().path("versao").asLong();
        assertThat(mantida.getBody().path("versao").asLong()).isGreaterThan(versaoAntiga);
        var dadosAntigos = dadosReceita(List.of(id, outra));
        dadosAntigos.put("versao", versaoAntiga);
        assertThat(dono.escrever(receita, PUT, dadosAntigos).getStatusCode()).isEqualTo(CONFLICT);
    }

    @Test
    void persisteOrdemPublicaERejeitaListaDesatualizadaOuRepetida() {
        var admin = entrarComoAdmin();
        var publico = new ClienteReceitopedia(porta);
        String primeira = idDe(adicionar(admin, "Z de ordem"));
        String segunda = idDe(adicionar(admin, "A de ordem"));
        var atual = new ArrayList<String>();
        var catalogo = publico.ler(CATEGORIAS);
        long versao = Long.parseLong(catalogo.getHeaders().getFirst("X-Versao-Categorias"));
        catalogo.getBody().forEach(tag -> atual.add(tag.path("id").asText()));
        assertThat(atual).endsWith(primeira, segunda);

        var novaOrdem = new ArrayList<>(atual);
        novaOrdem.remove(segunda);
        novaOrdem.add(0, segunda);
        var ordem = CATEGORIAS + "/ordem";
        assertThat(admin.escrever(ordem, PUT, Map.of("ids", novaOrdem, "versao", versao)).getStatusCode()).isEqualTo(OK);
        assertThat(publico.ler(CATEGORIAS).getBody()).extracting(tag -> tag.path("id").asText())
                .containsExactlyElementsOf(novaOrdem);

        assertThat(admin.escrever(ordem, PUT, Map.of("ids", List.of(segunda, segunda), "versao", versao)).getStatusCode())
                .isEqualTo(BAD_REQUEST);
        assertThat(admin.escrever(CATEGORIAS + "/" + primeira, DELETE, null).getStatusCode())
                .isEqualTo(NO_CONTENT);
        assertThat(admin.escrever(ordem, PUT, Map.of("ids", novaOrdem, "versao", versao)).getStatusCode()).isEqualTo(CONFLICT);
    }

    @Test
    void duasOrdenacoesDaMesmaVersaoGeramUmConflito() throws Exception {
        var primeiraAdmin = entrarComoAdmin();
        var segundaAdmin = entrarComoAdmin();
        assertThat(adicionar(primeiraAdmin, "Primeira para corrida").getStatusCode()).isEqualTo(CREATED);
        assertThat(adicionar(segundaAdmin, "Segunda para corrida").getStatusCode()).isEqualTo(CREATED);
        var catalogo = primeiraAdmin.ler(CATEGORIAS);
        var versao = Long.parseLong(catalogo.getHeaders().getFirst("X-Versao-Categorias"));
        var ids = new ArrayList<String>();
        catalogo.getBody().forEach(tag -> ids.add(tag.path("id").asText()));
        var inversa = new ArrayList<>(ids);
        java.util.Collections.reverse(inversa);
        var inicio = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> {
                inicio.await();
                return primeiraAdmin.escrever(CATEGORIAS + "/ordem", PUT, Map.of("ids", inversa, "versao", versao));
            });
            var b = executor.submit(() -> {
                inicio.await();
                return segundaAdmin.escrever(CATEGORIAS + "/ordem", PUT, Map.of("ids", ids, "versao", versao));
            });
            inicio.countDown();
            var respostaA = a.get(15, TimeUnit.SECONDS);
            var respostaB = b.get(15, TimeUnit.SECONDS);
            assertThat(List.of(respostaA.getStatusCode(), respostaB.getStatusCode())).containsExactlyInAnyOrder(OK, CONFLICT);
            var atual = primeiraAdmin.ler(CATEGORIAS);
            assertThat(atual.getHeaders().getFirst("X-Versao-Categorias")).isEqualTo(String.valueOf(versao + 1));
            assertThat(atual.getBody()).extracting(categoria -> categoria.path("id").asText())
                    .containsExactlyElementsOf(respostaA.getStatusCode().equals(OK) ? inversa : ids);
        } finally {
            executor.shutdownNow();
        }
    }

    private void rejeitarTags(ClienteReceitopedia cliente, List<String> ids, String mensagem) {
        var resposta = cliente.escrever("/backend/receitas", POST, dadosReceita(ids));
        assertThat(resposta.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(resposta.getBody().path("detail").asText()).contains(mensagem);
    }

    private static Map<String, Object> dadosReceita(List<String> ids) {
        return new LinkedHashMap<>(Map.of("titulo", "Bolo para festas", "ingredientes", List.of("Farinha"),
                "etapas", List.of("Misture os ingredientes."), "categorias", ids));
    }

    private static void conferirTags(JsonNode receita, String... ids) {
        assertThat(receita.path("categorias")).extracting(tag -> tag.path("id").asText()).containsExactly(ids);
    }

    private static String idDe(ResponseEntity<JsonNode> resposta) {
        return resposta.getBody().path("id").asText();
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
