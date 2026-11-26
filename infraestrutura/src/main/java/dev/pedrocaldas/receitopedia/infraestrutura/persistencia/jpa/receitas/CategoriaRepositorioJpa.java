package dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.receitas;

import java.util.List;
import java.util.Locale;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.pedrocaldas.receitopedia.aplicacao.receitas.CategoriaDuplicada;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.CategoriaRepositorio;
import dev.pedrocaldas.receitopedia.dominio.receitas.Categoria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
class CategoriaRepositorioJpa implements CategoriaRepositorio {
    @PersistenceContext
    private EntityManager entidades;

    @Override
    @Transactional
    public Categoria adicionar(Categoria categoria) {
        try {
            entidades.persist(new CategoriaJpa(categoria));
            entidades.flush();
            return categoria;
        } catch (ConstraintViolationException erro) {
            String restricao = String.valueOf(erro.getConstraintName()).toLowerCase(Locale.ROOT);
            if (restricao.contains("uk_categoria_nome_chave")) {
                throw new CategoriaDuplicada();
            }
            throw erro;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return entidades.createQuery("from CategoriaJpa c order by c.nomeChave", CategoriaJpa.class)
                .getResultList().stream().map(CategoriaJpa::paraDominio).toList();
    }
}
