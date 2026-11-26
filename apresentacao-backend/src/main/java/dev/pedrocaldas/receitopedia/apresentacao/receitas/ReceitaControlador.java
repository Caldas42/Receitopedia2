package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import dev.pedrocaldas.receitopedia.aplicacao.receitas.AlteracaoFotos;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.FotoNaoEncontrada;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaDesatualizada;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaNaoEncontrada;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaResumo;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaServicoAplicacao;
import dev.pedrocaldas.receitopedia.apresentacao.usuarios.UsuarioPrincipal;
import dev.pedrocaldas.receitopedia.dominio.receitas.AutorReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.DadosReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.ReceitaAcessoNegado;
import dev.pedrocaldas.receitopedia.dominio.receitas.Receita;
import dev.pedrocaldas.receitopedia.dominio.receitas.TempoPreparo;
import dev.pedrocaldas.receitopedia.dominio.receitas.SelecaoFotos;

@RestController
@RequestMapping("/backend/receitas")
public class ReceitaControlador {
    private final ReceitaServicoAplicacao servico;

    public ReceitaControlador(ReceitaServicoAplicacao servico) {
        this.servico = servico;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Receita> adicionar(@RequestBody Formulario formulario, @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return adicionar(formulario, usuario, null);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Receita> adicionarComFotos(@RequestPart("receita") Formulario formulario,
            @RequestPart(value = "fotos", required = false) List<MultipartFile> arquivos,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return adicionar(formulario, usuario, prepararFotos(formulario, arquivos));
    }

    private ResponseEntity<Receita> adicionar(Formulario formulario, UsuarioPrincipal usuario, AlteracaoFotos fotos) {
        var dono = new AutorReceita(usuario.id(), usuario.nomeUsuario());
        Receita receita = servico.adicionar(formulario.dados(), dono, fotos, formulario.categorias());
        return ResponseEntity.created(URI.create("/backend/receitas/" + receita.id())).body(receita);
    }

    @GetMapping
    public List<ReceitaResumo> listar() {
        return servico.listar();
    }

    @GetMapping("/minhas")
    public List<ReceitaResumo> minhas(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return servico.listarDoDono(usuario.id());
    }

    @GetMapping("/{id}")
    public Receita visualizar(@PathVariable UUID id) {
        return servico.visualizar(id);
    }

    @GetMapping("/{id}/fotos/{fotoId}")
    public ResponseEntity<byte[]> visualizarFoto(@PathVariable UUID id, @PathVariable UUID fotoId) {
        var foto = servico.visualizarFoto(id, fotoId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(foto.tipo())).body(foto.bytes());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Receita editar(@PathVariable UUID id, @RequestBody Formulario formulario,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return servico.editar(id, formulario.versao(), usuario.id(), formulario.dados(), null, formulario.categorias());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Receita editarComFotos(@PathVariable UUID id, @RequestPart("receita") Formulario formulario,
            @RequestPart(value = "fotos", required = false) List<MultipartFile> arquivos,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return servico.editar(id, formulario.versao(), usuario.id(), formulario.dados(),
                prepararFotos(formulario, arquivos), formulario.categorias());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id, @RequestParam Long versao,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        servico.remover(id, versao, usuario.id());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail dadosInvalidos(IllegalArgumentException erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, erro.getMessage());
    }

    @ExceptionHandler({ReceitaNaoEncontrada.class, FotoNaoEncontrada.class})
    ProblemDetail recursoNaoEncontrado(RuntimeException erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
    }

    @ExceptionHandler(ReceitaAcessoNegado.class)
    ProblemDetail acessoNegado(ReceitaAcessoNegado erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, erro.getMessage());
    }

    @ExceptionHandler(ReceitaDesatualizada.class)
    ProblemDetail receitaDesatualizada(ReceitaDesatualizada erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, erro.getMessage());
    }

    private AlteracaoFotos prepararFotos(Formulario formulario, List<MultipartFile> arquivos) {
        var mantidas = formulario.fotosMantidas() == null ? List.<UUID>of() : formulario.fotosMantidas();
        var uploads = arquivos == null ? List.<MultipartFile>of() : arquivos;
        SelecaoFotos.validarQuantidade(mantidas.size() + uploads.size());
        var bytes = new ArrayList<byte[]>();
        try {
            for (MultipartFile arquivo : uploads) {
                bytes.add(arquivo.getBytes());
            }
        } catch (IOException erro) {
            throw new UncheckedIOException("Não foi possível receber a foto.", erro);
        }
        return AlteracaoFotos.preparar(mantidas, bytes, formulario.fotoPrincipal());
    }

    record Formulario(String titulo, String descricao, List<String> ingredientes, List<String> etapas,
            Long versao, Integer horas, Integer minutos, List<UUID> fotosMantidas, Integer fotoPrincipal, List<UUID> categorias) {
        DadosReceita dados() {
            return new DadosReceita(titulo, descricao, ingredientes, etapas, TempoPreparo.informado(horas, minutos));
        }
    }
}
