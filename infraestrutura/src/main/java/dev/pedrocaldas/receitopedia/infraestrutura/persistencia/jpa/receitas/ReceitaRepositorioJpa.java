package dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.receitas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import dev.pedrocaldas.receitopedia.aplicacao.receitas.AlteracaoFotos;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.BuscaReceitas;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.PaginaReceitas;
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
        protegerSelecao(categorias);
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
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PaginaReceitas buscar(UUID donoId, BuscaReceitas busca) {
        var palavras = busca.palavras();
        var condicoes = new ArrayList<String>();
        if (donoId != null) {
            condicoes.add("receita.dono.id = :donoId");
        }
        for (int i = 0; i < palavras.size(); i++) {
            condicoes.add("""
                    (receita.tituloChave like :termo%d escape '!' or exists (
                        select 1 from ReceitaJpa vinculada join vinculada.categorias categoria
                        where vinculada.id = receita.id and categoria.nomeChave like :termo%d escape '!'
                    ) or dono.id in (
                        select autor.id from UsuarioJpa autor where autor.nomeChave like :termo%d escape '!'
                    ))
                    """.formatted(i, i, i));
        }
        String filtro = condicoes.isEmpty() ? "" : "where " + String.join(" and ", condicoes);
        var consulta = entidades.createQuery("""
                select new dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaResumo(
                    receita.id, receita.titulo, receita.descricao, receita.tempoMinutos, foto.id, dono.nomeUsuario)
                from ReceitaJpa receita
                left join receita.dono dono
                left join receita.fotos foto on index(foto) = 0
                %s
                order by lower(receita.titulo), receita.id
                """.formatted(filtro), ReceitaResumo.class);
        if (donoId != null) {
            consulta.setParameter("donoId", donoId);
        }
        for (int i = 0; i < palavras.size(); i++) {
            consulta.setParameter("termo" + i, trechoLiteral(palavras.get(i)));
        }

        int tamanho = BuscaReceitas.POR_PAGINA;
        var receitas = consulta.setFirstResult(busca.pagina() * tamanho).setMaxResults(tamanho + 1).getResultList();
        boolean temProxima = receitas.size() > tamanho;
        var pagina = temProxima ? receitas.subList(0, tamanho) : receitas;
        return new PaginaReceitas(incluirCategorias(pagina), temProxima);
    }

    private static String trechoLiteral(String palavra) {
        return "%" + palavra.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
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
        protegerSelecao(categorias);
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

    private void protegerSelecao(SelecaoCategorias selecao) {
        if (selecao != null && !selecao.ids().isEmpty()) {
            CatalogoCategoriasJpa.bloquear(entidades, LockModeType.PESSIMISTIC_READ);
        }
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
