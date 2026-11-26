package dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.receitas;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import dev.pedrocaldas.receitopedia.aplicacao.receitas.CatalogoAlterado;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.CategoriaDuplicada;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.CategoriaNaoEncontrada;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.CategoriaRepositorio;
import dev.pedrocaldas.receitopedia.dominio.receitas.Categoria;
import dev.pedrocaldas.receitopedia.dominio.receitas.OrdemCategorias;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@Repository
class CategoriaRepositorioJpa implements CategoriaRepositorio {
    @PersistenceContext
    private EntityManager entidades;

    @Override
    @Transactional
    public Categoria adicionar(Categoria categoria) {
        CatalogoCategoriasJpa.bloquear(entidades, LockModeType.PESSIMISTIC_WRITE).registrarAlteracao();
        long posicao = entidades.createQuery("select coalesce(max(c.posicao), -1L) + 1L from CategoriaJpa c", Long.class)
                .getSingleResult();
        entidades.persist(new CategoriaJpa(categoria, posicao));
        confirmarNomeUnico();
        return categoria;
    }

    @Override
    @Transactional
    public Categoria editar(Categoria categoria) {
        CatalogoCategoriasJpa.bloquear(entidades, LockModeType.PESSIMISTIC_WRITE).registrarAlteracao();
        carregar(categoria.id()).renomear(categoria);
        confirmarNomeUnico();
        return categoria;
    }

    @Override
    @Transactional
    public void remover(UUID id) {
        CatalogoCategoriasJpa.bloquear(entidades, LockModeType.PESSIMISTIC_WRITE).registrarAlteracao();
        var categoria = carregar(id);
        entidades.createNativeQuery("""
                update receita set versao = versao + 1
                where id in (select receita_id from receita_categoria where categoria_id = :id)
                """).setParameter("id", id).executeUpdate();
        entidades.createNativeQuery("delete from receita_categoria where categoria_id = :id")
                .setParameter("id", id).executeUpdate();
        entidades.remove(categoria);
    }

    @Override
    @Transactional
    public Catalogo reordenar(OrdemCategorias ordem, long versao) {
        var estado = CatalogoCategoriasJpa.bloquear(entidades, LockModeType.PESSIMISTIC_WRITE);
        if (estado.versao() != versao) {
            throw new CatalogoAlterado();
        }
        var catalogo = new HashMap<UUID, CategoriaJpa>();
        registros().forEach(categoria -> catalogo.put(categoria.id(), categoria));
        if (!ordem.correspondeAoCatalogo(catalogo.keySet())) {
            throw new CatalogoAlterado();
        }
        int posicao = 0;
        for (UUID id : ordem.ids()) {
            catalogo.get(id).posicionar(posicao++);
        }
        estado.registrarAlteracao();
        return new Catalogo(ordem.ids().stream().map(catalogo::get).map(CategoriaJpa::paraDominio).toList(),
                estado.versao());
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Catalogo listar() {
        long versao = CatalogoCategoriasJpa.bloquear(entidades, LockModeType.NONE).versao();
        return new Catalogo(registros().stream().map(CategoriaJpa::paraDominio).toList(), versao);
    }

    private List<CategoriaJpa> registros() {
        return entidades.createQuery("from CategoriaJpa c order by c.posicao", CategoriaJpa.class).getResultList();
    }

    private CategoriaJpa carregar(UUID id) {
        var categoria = entidades.find(CategoriaJpa.class, id);
        if (categoria == null) {
            throw new CategoriaNaoEncontrada();
        }
        return categoria;
    }

    private void confirmarNomeUnico() {
        try {
            entidades.flush();
        } catch (ConstraintViolationException erro) {
            String restricao = String.valueOf(erro.getConstraintName()).toLowerCase(Locale.ROOT);
            if (restricao.contains("uk_categoria_nome_chave")) {
                throw new CategoriaDuplicada();
            }
            throw erro;
        }
    }
}
