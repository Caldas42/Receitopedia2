package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;

import dev.pedrocaldas.receitopedia.BackendAplicacao;
import dev.pedrocaldas.receitopedia.apresentacao.ClienteReceitopedia;
import dev.pedrocaldas.receitopedia.dominio.receitas.TipoFoto;

@SpringBootTest(classes = BackendAplicacao.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=${RECEITAS_TEST_DB_URL:jdbc:h2:mem:fotos;MODE=PostgreSQL}",
                "spring.datasource.username=${RECEITAS_TEST_DB_USER:sa}",
                "spring.datasource.password=${RECEITAS_TEST_DB_PASSWORD:}"
        })
class FotosHttpTest {
    private static final String RECEITAS = "/backend/receitas";

    @TempDir
    static Path diretorio;

    @LocalServerPort
    private int porta;
    private ClienteReceitopedia dono;

    @DynamicPropertySource
    static void configurarDiretorio(DynamicPropertyRegistry propriedades) {
        propriedades.add("receitopedia.fotos.diretorio", () -> diretorio.toString());
    }

    @BeforeEach
    void criarSessao() {
        dono = new ClienteReceitopedia(porta);
        dono.criarContaESessao();
    }

    @Test
    void guardaFotosPorUuidMostraSoAPrincipalNaListaELimpaArquivosOrfaos() throws IOException {
        byte[] jpeg = gerarImagem("jpeg");
        byte[] png = gerarImagem("png");
        byte[] webp = Base64.getDecoder().decode("UklGRiQAAABXRUJQVlA4IBgAAAAwAQCdASoCAAIAAUAmJaQAA3AA/vz0AAA=");
        var criada = enviar(dono, POST, RECEITAS, dados(1), List.of(jpeg, png, webp));
        assertThat(criada.getStatusCode()).isEqualTo(CREATED);

        var receita = criada.getBody();
        String id = receita.path("id").asText();
        String caminho = RECEITAS + "/" + id;
        var fotos = receita.path("fotos");
        assertThat(fotos).extracting(foto -> foto.path("tipo").asText()).containsExactly("PNG", "JPEG", "WEBP");
        assertThat(arquivosNoDisco()).containsExactlyInAnyOrder(
                fotos.get(0).path("id").asText(), fotos.get(1).path("id").asText(), fotos.get(2).path("id").asText());

        var visitante = new ClienteReceitopedia(porta);
        var fotoPrincipal = fotos.get(0).path("id").asText();
        assertThat(visitante.ler(RECEITAS).getBody()).anySatisfy(resumo -> {
            assertThat(resumo.path("id").asText()).isEqualTo(id);
            assertThat(resumo.path("fotoPrincipalId").asText()).isEqualTo(fotoPrincipal);
            assertThat(resumo.has("fotos")).isFalse();
        });
        var baixada = visitante.baixar(caminho + "/fotos/" + fotoPrincipal);
        assertThat(baixada.getStatusCode()).isEqualTo(OK);
        assertThat(baixada.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(baixada.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(baixada.getBody()).isEqualTo(png);

        var edicao = dados(1);
        edicao.put("versao", receita.path("versao").asLong());
        String mantida = fotos.get(2).path("id").asText();
        edicao.put("fotosMantidas", List.of(mantida));
        var outro = new ClienteReceitopedia(porta);
        outro.criarContaESessao();
        assertThat(enviar(outro, PUT, caminho, edicao, List.of(png)).getStatusCode()).isEqualTo(FORBIDDEN);
        assertThat(arquivosNoDisco()).hasSize(3);

        var editada = enviar(dono, PUT, caminho, edicao, List.of(png));
        assertThat(editada.getStatusCode()).isEqualTo(OK);
        var atuais = editada.getBody().path("fotos");
        String novaPrincipal = atuais.get(0).path("id").asText();
        assertThat(atuais.get(1).path("id").asText()).isEqualTo(mantida);
        assertThat(arquivosNoDisco()).containsExactlyInAnyOrder(novaPrincipal, mantida);
        assertThat(visitante.baixar(caminho + "/fotos/" + fotoPrincipal).getStatusCode()).isEqualTo(NOT_FOUND);

        long versao = editada.getBody().path("versao").asLong();
        var exclusao = dono.escrever(caminho + "?versao=" + versao, DELETE, null);
        assertThat(exclusao.getStatusCode()).isEqualTo(NO_CONTENT);
        assertThat(arquivosNoDisco()).isEmpty();
    }

    @Test
    void impedeMaisDeCincoFotosArquivosGrandesEAssinaturaInvalida() throws IOException {
        byte[] png = gerarImagem("png");
        var semFoto = enviar(dono, POST, RECEITAS, dados(null), List.of());
        assertThat(semFoto.getStatusCode()).isEqualTo(CREATED);
        assertThat(semFoto.getBody().path("fotos")).isEmpty();
        var excesso = enviar(dono, POST, RECEITAS, dados(0), List.of(png, png, png, png, png, png));
        assertThat(excesso.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(excesso.getBody().path("detail").asText()).contains("5 fotos");
        for (int tamanho : List.of(TipoFoto.LIMITE_BYTES + 1, 2 * TipoFoto.LIMITE_BYTES)) {
            var grande = enviar(dono, POST, RECEITAS, dados(0), List.of(new byte[tamanho]));
            assertThat(grande.getStatusCode()).isEqualTo(PAYLOAD_TOO_LARGE);
            assertThat(grande.getBody().path("detail").asText()).contains("5 MB");
        }
        var falso = enviar(dono, POST, RECEITAS, dados(0), List.of("<html></html>".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThat(falso.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(falso.getBody().path("detail").asText()).contains("JPEG, PNG ou WebP");
        assertThat(arquivosNoDisco()).isEmpty();
    }

    private List<String> arquivosNoDisco() throws IOException {
        try (var arquivos = Files.list(diretorio)) {
            return arquivos.map(arquivo -> arquivo.getFileName().toString()).toList();
        }
    }

    private Map<String, Object> dados(Integer principal) {
        var dados = new LinkedHashMap<String, Object>();
        dados.put("ingredientes", List.of("Tomate"));
        dados.put("etapas", List.of("Corte o tomate."));
        dados.put("fotoPrincipal", principal);
        return dados;
    }

    private ResponseEntity<JsonNode> enviar(ClienteReceitopedia cliente, HttpMethod metodo, String caminho,
            Map<String, Object> dados, List<byte[]> fotos) {
        var partes = new LinkedMultiValueMap<String, Object>();
        var json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);
        partes.add("receita", new HttpEntity<>(dados, json));

        var alegado = new HttpHeaders();
        alegado.setContentType(MediaType.IMAGE_PNG);
        for (byte[] foto : fotos) {
            var conteudo = new ByteArrayResource(foto) {
                @Override
                public String getFilename() {
                    return "../../imagem.png";
                }
            };
            partes.add("fotos", new HttpEntity<>(conteudo, alegado));
        }
        return cliente.escrever(caminho, metodo, partes);
    }

    private byte[] gerarImagem(String formato) throws IOException {
        var bytes = new ByteArrayOutputStream();
        boolean gerada = ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), formato, bytes);
        assertThat(gerada).isTrue();
        return bytes.toByteArray();
    }
}
