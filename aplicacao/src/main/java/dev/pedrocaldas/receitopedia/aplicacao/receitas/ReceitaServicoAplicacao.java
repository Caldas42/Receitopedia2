package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.AutorReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.DadosReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.Receita;
import dev.pedrocaldas.receitopedia.dominio.receitas.SelecaoCategorias;

public class ReceitaServicoAplicacao {
    private final ReceitaRepositorio repositorio;
    private final ArmazenamentoFotos armazenamento;

    public ReceitaServicoAplicacao(ReceitaRepositorio repositorio, ArmazenamentoFotos armazenamento) {
        this.repositorio = repositorio;
        this.armazenamento = armazenamento;
    }

    public Receita adicionar(DadosReceita dados, AutorReceita dono, AlteracaoFotos fotos, List<UUID> categorias) {
        return repositorio.adicionar(Receita.nova(dados, dono), fotos, selecionarCategorias(categorias));
    }

    public PaginaReceitas listar(BuscaReceitas busca) {
        return repositorio.buscar(null, busca);
    }

    public PaginaReceitas listarDoDono(UUID donoId, BuscaReceitas busca) {
        return repositorio.buscar(Objects.requireNonNull(donoId), busca);
    }

    public Receita visualizar(UUID id) {
        return repositorio.encontrar(id).orElseThrow(ReceitaNaoEncontrada::new);
    }

    public Receita editar(UUID id, Long versao, UUID usuarioId, DadosReceita dados, AlteracaoFotos fotos,
            List<UUID> categorias) {
        return repositorio.editar(id, validarVersao(versao), usuarioId, dados, fotos, selecionarCategorias(categorias));
    }

    public void remover(UUID id, Long versao, UUID usuarioId) {
        repositorio.remover(id, validarVersao(versao), usuarioId);
    }

    public FotoDisponivel visualizarFoto(UUID receitaId, UUID fotoId) {
        var foto = visualizar(receitaId).fotos().stream()
                .filter(candidata -> candidata.id().equals(fotoId))
                .findFirst()
                .orElseThrow(FotoNaoEncontrada::new);
        byte[] bytes = armazenamento.ler(fotoId).orElseThrow(FotoNaoEncontrada::new);
        return new FotoDisponivel(foto.tipo().mime(), bytes);
    }

    public record FotoDisponivel(String tipo, byte[] bytes) {
    }

    private static SelecaoCategorias selecionarCategorias(List<UUID> ids) {
        return ids == null ? null : new SelecaoCategorias(ids);
    }

    private static long validarVersao(Long versao) {
        if (versao == null || versao < 0) {
            throw new IllegalArgumentException("Informe uma versão válida da receita.");
        }
        return versao;
    }
}
