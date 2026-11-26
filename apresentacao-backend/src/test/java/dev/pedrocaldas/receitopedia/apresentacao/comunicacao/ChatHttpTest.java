package dev.pedrocaldas.receitopedia.apresentacao.comunicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.HttpStatus.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;

import dev.pedrocaldas.receitopedia.BackendAplicacao;
import dev.pedrocaldas.receitopedia.apresentacao.ClienteReceitopedia;

@SpringBootTest(classes = BackendAplicacao.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=${CHAT_TEST_DB_URL:jdbc:h2:mem:chat;MODE=PostgreSQL}",
                "spring.datasource.username=${CHAT_TEST_DB_USER:sa}",
                "spring.datasource.password=${CHAT_TEST_DB_PASSWORD:}"
        })
class ChatHttpTest {
    private static final String CONVERSAS = "/backend/chat/conversas";

    @LocalServerPort
    private int porta;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void enviaEmOrdemSemDuplicarETrataBuscaLiteralEAutorizacao() throws Exception {
        var a = conta();
        var b = conta();
        var c = conta();
        String chat = dm(a, b);
        String rota = CONVERSAS + "/" + chat + "/mensagens";
        assertThat(new ClienteReceitopedia(porta).ler("/backend/chat/eventos").getStatusCode()).isEqualTo(UNAUTHORIZED);
        assertThat(c.cliente.ler(rota).getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(c.cliente.ler(CONVERSAS + "/" + UUID.randomUUID() + "/mensagens").getBody().path("detail"))
                .isEqualTo(c.cliente.ler(rota).getBody().path("detail"));

        var id1 = UUID.randomUUID();
        var id2 = UUID.randomUUID();
        var estados = simultaneos(() -> enviar(a, rota, id1, " Pão 100% _ "), () -> enviar(a, rota, id2, "Pão 100A Z"));
        assertThat(estados).containsExactly(ACCEPTED, ACCEPTED);
        assertThat(enviar(a, rota, id1, "repetição")).isEqualTo(ACCEPTED);
        var lista = a.cliente.ler(rota).getBody();
        assertThat(lista).extracting(m -> m.path("sequencia").asInt()).containsExactly(2, 1);
        assertThat(a.cliente.ler(rota + "?antes=2").getBody()).hasSize(1);
        assertThat(a.cliente.ler(rota + "?busca={termo}", "pao 100% _").getBody())
                .extracting(m -> m.path("id").asText()).containsExactly(id1.toString());
        assertThat(b.cliente.ler(rota).getBody()).isEmpty();
    }

    @Test
    void limitaGrupoMesmoComEntradasSimultaneasEProtegeAdministracao() throws Exception {
        var admin = conta();
        var comum = conta();
        var prefixo = "M" + UUID.randomUUID().toString().substring(0, 8);
        var nomes = IntStream.range(0, 255).mapToObj(i -> prefixo + i).toList();
        jdbc.batchUpdate("insert into usuario (id,email,nome_usuario,nome_chave,senha_hash,papel) values (?,?,?,?,?,'USER')",
                nomes.stream().map(nome -> new Object[]{UUID.randomUUID(), nome.toLowerCase(java.util.Locale.ROOT)
                    + "@example.com", nome, nome.toLowerCase(java.util.Locale.ROOT), "somente-teste"}).toList());
        var iniciais = new java.util.ArrayList<>(nomes.subList(0, 253));
        iniciais.add(comum.nome);
        String grupo = criarGrupo(admin, iniciais);
        String rota = CONVERSAS + "/" + grupo + "/grupo";
        assertThat(comum.cliente.escrever(rota, PUT, Map.of("nome", "Sem autorização")).getStatusCode()).isEqualTo(FORBIDDEN);
        assertThat(admin.cliente.escrever(rota + "/eu", DELETE, null).getStatusCode()).isEqualTo(CONFLICT);
        var estados = simultaneos(
                () -> admin.cliente.escrever(rota + "/membros", POST, Map.of("usuario", nomes.get(253))).getStatusCode(),
                () -> admin.cliente.escrever(rota + "/membros", POST, Map.of("usuario", nomes.get(254))).getStatusCode());
        assertThat(estados).containsExactlyInAnyOrder(NO_CONTENT, CONFLICT);
        assertThat(admin.cliente.ler(rota).getBody().path("membros")).hasSize(256);
        assertThat(admin.cliente.escrever(rota + "/membros/" + comum.id + "/admin", PUT, null).getStatusCode())
                .isEqualTo(NO_CONTENT);
        assertThat(admin.cliente.escrever(rota + "/eu", DELETE, null).getStatusCode()).isEqualTo(NO_CONTENT);
        assertThat(admin.cliente.ler(rota).getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(comum.cliente.escrever(rota, PUT, Map.of("nome", "Renomeado")).getStatusCode()).isEqualTo(NO_CONTENT);
    }

    @Test
    void sincronizaLeiturasEConflitaEdicaoComExclusao() throws Exception {
        var autor = conta();
        var b = conta();
        var c = conta();
        String grupo = criarGrupo(autor, List.of(b.nome, c.nome));
        String raiz = CONVERSAS + "/" + grupo;
        String rota = raiz + "/mensagens";
        UUID id = UUID.randomUUID();
        assertThat(enviar(autor, rota, id, "Mensagem inicial")).isEqualTo(ACCEPTED);
        jdbc.update("update chat_mensagem set processada = true where id = ?", id);
        var recibo = Map.of("ids", List.of(id));
        b.cliente.escrever(raiz + "/leituras", POST, recibo);
        assertThat(autor.cliente.ler(rota).getBody().get(0).path("status").asText()).isEqualTo("ENTREGUE");
        assertThat(simultaneos(() -> b.cliente.escrever(raiz + "/leituras", POST, recibo).getStatusCode(),
                () -> c.cliente.escrever(raiz + "/leituras", POST, recibo).getStatusCode()))
                .containsExactly(NO_CONTENT, NO_CONTENT);
        assertThat(autor.cliente.ler(rota).getBody().get(0).path("status").asText()).isEqualTo("LIDA");
        assertThat(jdbc.queryForObject("select count(*) from chat_recibo where mensagem_id = ?", Integer.class, id))
                .isEqualTo(2);
        String recurso = rota + "/" + id;
        assertThat(b.cliente.escrever(recurso, PUT, Map.of("versao", 0, "texto", "Alheio")).getStatusCode())
                .isEqualTo(FORBIDDEN);
        assertThat(simultaneos(() -> autor.cliente.escrever(recurso, PUT, Map.of("versao", 0, "texto", "Editada")).getStatusCode(),
                () -> autor.cliente.escrever(recurso + "?versao=0", DELETE, null).getStatusCode()))
                .containsExactlyInAnyOrder(OK, CONFLICT);
    }

    @Test
    void bloqueiaDmNosDoisSentidosAteDesbloquear() {
        var a = conta();
        var b = conta();
        String chat = dm(a, b);
        String bloqueio = "/backend/chat/bloqueios/" + b.id;
        assertThat(a.cliente.escrever(bloqueio, PUT, null).getStatusCode()).isEqualTo(NO_CONTENT);
        String rota = CONVERSAS + "/" + chat + "/mensagens";
        assertThat(enviar(a, rota, UUID.randomUUID(), "A")).isEqualTo(FORBIDDEN);
        assertThat(enviar(b, rota, UUID.randomUUID(), "B")).isEqualTo(FORBIDDEN);
        assertThat(a.cliente.escrever(bloqueio, DELETE, null).getStatusCode()).isEqualTo(NO_CONTENT);
        assertThat(enviar(a, rota, UUID.randomUUID(), "A")).isEqualTo(ACCEPTED);
    }

    @Test
    void seguimentosSimultaneosCriamUmaDmEPreservamHistoricoAoDesfazerAmizade() throws Exception {
        var a = conta();
        var b = conta();
        var c = conta();
        assertThat(new ClienteReceitopedia(porta).escrever(seguimento(b), PUT, null).getStatusCode())
                .isEqualTo(UNAUTHORIZED);
        assertThat(a.cliente.escrever(seguimento(a), PUT, null).getStatusCode()).isEqualTo(BAD_REQUEST);

        var resultados = simultaneos(() -> a.cliente.escrever(seguimento(b), PUT, null).getStatusCode(),
                () -> b.cliente.escrever(seguimento(a), PUT, null).getStatusCode());
        assertThat(resultados).containsExactly(NO_CONTENT, NO_CONTENT);
        String conversa = a.cliente.ler(CONVERSAS).getBody().get(0).path("id").asText();
        assertThat(b.cliente.ler(CONVERSAS).getBody()).singleElement()
                .satisfies(dm -> assertThat(dm.path("id").asText()).isEqualTo(conversa));
        UUID aId = UUID.fromString(a.id);
        UUID bId = UUID.fromString(b.id);
        assertThat(jdbc.queryForObject("""
                select count(*) from chat_conversa where dm_primeiro in (?, ?) and dm_segundo in (?, ?)
                """, Long.class, aId, bId, aId, bId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from evento_outbox where tipo='dm.criada' and agregado=?",
                Long.class, UUID.fromString(conversa))).isEqualTo(1);

        assertThat(a.cliente.escrever(seguimento(b), PUT, null).getStatusCode()).isEqualTo(NO_CONTENT);
        assertThat(jdbc.queryForObject("select count(*) from seguimento where seguidor_id=? and seguido_id=?",
                Long.class, aId, bId)).isEqualTo(1);
        a.cliente.escrever(seguimento(c), PUT, null);
        assertThat(c.cliente.ler(CONVERSAS).getBody()).isEmpty();

        String historico = CONVERSAS + "/" + conversa + "/mensagens";
        UUID mensagem = UUID.randomUUID();
        assertThat(enviar(a, historico, mensagem, "Histórico preservado")).isEqualTo(ACCEPTED);
        assertThat(a.cliente.escrever(seguimento(b), DELETE, null).getStatusCode()).isEqualTo(NO_CONTENT);
        assertThat(a.cliente.escrever(seguimento(b), DELETE, null).getStatusCode()).isEqualTo(NO_CONTENT);
        assertThat(a.cliente.ler(CONVERSAS).getBody()).isEmpty();
        assertThat(b.cliente.ler(CONVERSAS).getBody()).isEmpty();
        assertThat(b.cliente.ler(historico).getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(enviar(b, historico, UUID.randomUUID(), "Bloqueada")).isEqualTo(NOT_FOUND);
        assertThat(a.cliente.escrever(seguimento(b), PUT, null).getStatusCode()).isEqualTo(NO_CONTENT);
        assertThat(a.cliente.ler(CONVERSAS).getBody().get(0).path("id").asText()).isEqualTo(conversa);
        assertThat(a.cliente.ler(historico).getBody()).extracting(m -> m.path("id").asText())
                .containsExactly(mensagem.toString());
    }

    @Test
    void filtraListasDePessoasPorTrechoLiteralEPaginaPorCursor() {
        var ator = conta();
        var prefixo = "Busca" + UUID.randomUUID().toString().substring(0, 6);
        var usuarios = IntStream.range(0, 28).mapToObj(i -> new Conta(null, prefixo + "_" + (100 + i),
                UUID.randomUUID().toString())).toList();
        jdbc.batchUpdate("insert into usuario (id,email,nome_usuario,nome_chave,senha_hash,papel) values (?,?,?,?,?,'USER')",
                usuarios.stream().map(u -> new Object[]{UUID.fromString(u.id),
                    u.nome.toLowerCase(java.util.Locale.ROOT) + "@example.com", u.nome,
                    u.nome.toLowerCase(java.util.Locale.ROOT), "somente-teste"}).toList());
        var primeira = usuarios.get(0);
        var segunda = usuarios.get(1);
        ator.cliente.escrever(seguimento(primeira), PUT, null);
        String raiz = "/backend/pessoas?lista=";
        String busca = "Búsca" + prefixo.substring(5) + "_";
        var seguidas = ator.cliente.ler(raiz + "SEGUINDO&busca={busca}", busca).getBody();
        assertThat(seguidas.path("pessoas")).extracting(pessoa -> pessoa.path("id").asText()).containsExactly(primeira.id);
        var pagina = ator.cliente.ler(raiz + "TODOS&busca={busca}", busca).getBody();
        assertThat(pagina.path("pessoas")).hasSize(25);
        assertThat(pagina.path("pessoas").get(0).path("id").asText()).isEqualTo(segunda.id);
        var resto = ator.cliente.ler(raiz + "TODOS&busca={busca}&cursor={cursor}", busca, pagina.path("proximo").asText()).getBody();
        assertThat(resto.path("pessoas")).extracting(pessoa -> pessoa.path("id").asText())
                .containsExactly(usuarios.get(26).id, usuarios.get(27).id);
        assertThat(resto.path("proximo").isNull()).isTrue();
        assertThat(ator.cliente.ler(raiz + "TODOS&busca={busca}", "%").getBody().path("pessoas")).isEmpty();
        assertThat(ator.cliente.ler(raiz + "TODOS&busca={busca}", ator.nome).getBody().path("pessoas")).isEmpty();
        assertThat(ator.cliente.ler(raiz + "AMIGOS&busca={busca}", busca).getBody().path("pessoas")).isEmpty();
    }

    private String seguimento(Conta alvo) {
        return "/backend/pessoas/" + alvo.id + "/seguimento";
    }

    private HttpStatusCode enviar(Conta conta, String rota, UUID id, String texto) {
        return conta.cliente.escrever(rota, POST, Map.of("id", id, "texto", texto)).getStatusCode();
    }

    private String dm(Conta a, Conta b) {
        assertThat(a.cliente.escrever("/backend/pessoas/" + b.id + "/seguimento", PUT, null).getStatusCode())
                .isEqualTo(NO_CONTENT);
        assertThat(b.cliente.escrever("/backend/pessoas/" + a.id + "/seguimento", PUT, null).getStatusCode())
                .isEqualTo(NO_CONTENT);
        return a.cliente.ler(CONVERSAS).getBody().get(0).path("id").asText();
    }

    private String criarGrupo(Conta ator, List<String> membros) {
        var resposta = ator.cliente.escrever(CONVERSAS + "/grupos", POST, Map.of("nome", "Cozinha", "usuarios", membros));
        assertThat(resposta.getStatusCode()).isEqualTo(CREATED);
        return resposta.getBody().path("id").asText();
    }

    private Conta conta() {
        var cliente = new ClienteReceitopedia(porta);
        String nome = cliente.criarContaESessao();
        return new Conta(cliente, nome, cliente.ler("/backend/sessao").getBody().path("id").asText());
    }

    private List<HttpStatusCode> simultaneos(Callable<HttpStatusCode> a, Callable<HttpStatusCode> b) throws Exception {
        var inicio = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var x = executor.submit(() -> { inicio.await(); return a.call(); });
            var y = executor.submit(() -> { inicio.await(); return b.call(); });
            inicio.countDown();
            return List.of(x.get(15, TimeUnit.SECONDS), y.get(15, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    record Conta(ClienteReceitopedia cliente, String nome, String id) {}
}
