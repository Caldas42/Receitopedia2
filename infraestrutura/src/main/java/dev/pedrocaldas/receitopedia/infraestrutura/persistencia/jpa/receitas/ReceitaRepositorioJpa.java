package dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.receitas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import dev.pedrocaldas.receitopedia.aplicacao.receitas.AlteracaoFotos;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaDesatualizada;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaNaoEncontrada;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaRepositorio;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaResumo;
import dev.pedrocaldas.receitopedia.dominio.receitas.AutorReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.Categoria;
import dev.pedrocaldas.receitopedia.dominio.receitas.DadosReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.Receita;
import dev.pedrocaldas.receitopedia.dominio.receitas.SelecaoCategorias;
import dev.pedrocaldas.receitopedia.infraestrutura.fotos.FotosEmDisco;
import dev.pedrocaldas.receitopedia.infraestrutura.fotos.LimpezaFotos;
import dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.usuarios.UsuarioJpa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

@Repository
@Transactional(readOnly = true)
public class ReceitaRepositorioJpa implements ReceitaRepositorio {
    private final FotosEmDisco disco;
    private final LimpezaFotos limpeza;

    public ReceitaRepositorioJpa(FotosEmDisco disco, LimpezaFotos limpeza) {
        this.disco = disco;
        this.limpeza = limpeza;
    }

    @PersistenceContext
    private EntityManager entidades;

    @Override
    @Transactional
    public Receita adicionar(Receita receita, AlteracaoFotos fotos, SelecaoCategorias categorias) {
        ReceitaJpa registro = new ReceitaJpa(receita, entidades.getReference(UsuarioJpa.class, receita.dono().id()));
        if (fotos != null) {
            registro.atualizarFotos(fotos.selecao().aplicar(List.of()));
        }
        atribuirCategorias(registro, categorias);
        entidades.persist(registro);
        entidades.flush();
        if (fotos != null) {
            disco.gravar(fotos.uploads());
        }
        return registro.paraDominio();
    }

    @Override
    public List<ReceitaResumo> listar() {
        return listar(null);
    }

    @Override
    public List<ReceitaResumo> listarDoDono(UUID donoId) {
        return listar(Objects.requireNonNull(donoId));
    }

    private List<ReceitaResumo> listar(UUID donoId) {
        String filtro = donoId == null ? "" : "where receita.dono.id = :donoId";
        var consulta = entidades.createQuery("""
                select new dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaResumo(
                    receita.id, receita.titulo, receita.descricao, receita.tempoMinutos, foto.id)
                from ReceitaJpa receita
                left join receita.fotos foto on index(foto) = 0
                %s
                order by lower(receita.titulo), receita.id
                """.formatted(filtro), ReceitaResumo.class);
        if (donoId != null) {
            consulta.setParameter("donoId", donoId);
        }
        return incluirCategorias(consulta.getResultList());
    }

    private List<ReceitaResumo> incluirCategorias(List<ReceitaResumo> receitas) {
        if (receitas.isEmpty()) {
            return receitas;
        }
        var linhas = entidades.createQuery("""
                select receita.id as receitaId, categoria as categoria
                from ReceitaJpa receita join receita.categorias categoria
                where receita.id in :receitas
                order by categoria.nomeChave
                """, Tuple.class).setParameter("receitas", receitas.stream().map(ReceitaResumo::id).toList())
                .getResultList();
        var categoriasPorReceita = new HashMap<UUID, List<Categoria>>();
        for (var linha : linhas) {
            var id = linha.get("receitaId", UUID.class);
            var categoria = linha.get("categoria", CategoriaJpa.class).paraDominio();
            categoriasPorReceita.computeIfAbsent(id, chave -> new ArrayList<>()).add(categoria);
        }
        return receitas.stream()
                .map(receita -> receita.comCategorias(categoriasPorReceita.getOrDefault(receita.id(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Optional<Receita> encontrar(UUID id) {
        return Optional.ofNullable(entidades.find(ReceitaJpa.class, id)).map(ReceitaJpa::paraDominio);
    }

    @Override
    @Transactional
    public Receita editar(UUID id, long versao, UUID usuarioId, DadosReceita dados, AlteracaoFotos fotos,
            SelecaoCategorias categorias) {
        ReceitaJpa registro = carregarNaVersao(id, versao, usuarioId, LockModeType.NONE);
        atribuirCategorias(registro, categorias);
        registro.preencher(dados);
        if (fotos == null) {
            confirmarAlteracao();
        } else {
            var antigas = registro.fotos();
            var atuais = fotos.selecao().aplicar(antigas);
            registro.atualizarFotos(atuais);
            confirmarAlteracao();
            disco.gravar(fotos.uploads());
            limpeza.removerAposCommit(antigas.stream().filter(foto -> !atuais.contains(foto)).toList());
        }
        return registro.paraDominio();
    }

    @Override
    @Transactional
    public void remover(UUID id, long versao, UUID usuarioId) {
        var registro = carregarNaVersao(id, versao, usuarioId, LockModeType.PESSIMISTIC_WRITE);
        var fotos = registro.fotos();
        entidades.remove(registro);
        confirmarAlteracao();
        limpeza.removerAposCommit(fotos);
    }

    private void atribuirCategorias(ReceitaJpa registro, SelecaoCategorias selecao) {
        if (selecao == null) {
            return;
        }
        List<CategoriaJpa> categorias = selecao.ids().isEmpty() ? List.of()
                : entidades.createQuery("from CategoriaJpa c where c.id in :ids order by c.nomeChave", CategoriaJpa.class)
                        .setParameter("ids", selecao.ids()).getResultList();
        selecao.conferirExistentes(categorias.size());
        registro.atualizarCategorias(categorias);
    }

    private ReceitaJpa carregarNaVersao(UUID id, long versao, UUID usuarioId, LockModeType bloqueio) {
        ReceitaJpa registro = entidades.find(ReceitaJpa.class, id, bloqueio);
        if (registro == null) {
            throw new ReceitaNaoEncontrada();
        }
        AutorReceita.exigirAutoria(registro.autor(), usuarioId);
        if (registro.versao() != versao) {
            throw new ReceitaDesatualizada();
        }
        return registro;
    }

    private void confirmarAlteracao() {
        try {
            entidades.flush();
        } catch (OptimisticLockException e) {
            throw new ReceitaDesatualizada();
        }
    }
}
