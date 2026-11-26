package dev.pedrocaldas.receitopedia.apresentacao;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

public class ClienteReceitopedia {
    public static final String SENHA_TESTE = "Receita-Teste1!";
    private final TestRestTemplate http;

    public ClienteReceitopedia(int porta) {
        http = new TestRestTemplate(new RestTemplateBuilder()
                .rootUri("http://localhost:" + porta)
                .requestFactory(() -> new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build())));
    }

    public ResponseEntity<JsonNode> ler(String caminho) {
        return http.getForEntity(caminho, JsonNode.class);
    }

    public ResponseEntity<JsonNode> escrever(String caminho, HttpMethod metodo, Object corpo) {
        var token = ler("/backend/csrf").getBody().path("token").asText();
        var cabecalhos = new HttpHeaders();
        cabecalhos.set("X-CSRF-TOKEN", token);
        return http.exchange(caminho, metodo, new HttpEntity<>(corpo, cabecalhos), JsonNode.class);
    }

    public ResponseEntity<JsonNode> escreverSemCsrf(String caminho, Object corpo) {
        return http.postForEntity(caminho, corpo, JsonNode.class);
    }

    public ResponseEntity<JsonNode> cadastrar(String email, String nome, String senha) {
        return escrever("/backend/usuarios", POST, Map.of("email", email, "nomeUsuario", nome, "senha", senha));
    }

    public ResponseEntity<JsonNode> entrar(String email, String senha) {
        var dados = new LinkedMultiValueMap<String, String>();
        dados.add("email", email);
        dados.add("senha", senha);
        return escrever("/backend/login", POST, dados);
    }

    public String criarContaESessao() {
        String nome = "Pessoa_" + UUID.randomUUID().toString().substring(0, 12);
        String email = nome + "@example.com";
        assertEquals(CREATED, cadastrar(email, nome, SENHA_TESTE).getStatusCode());
        assertEquals(NO_CONTENT, entrar(email, SENHA_TESTE).getStatusCode());
        return nome;
    }
}
