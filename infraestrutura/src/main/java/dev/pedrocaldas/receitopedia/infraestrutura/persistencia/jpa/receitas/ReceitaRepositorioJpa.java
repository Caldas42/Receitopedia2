package dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.receitas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaDesatualizada;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaNaoEncontrada;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaRepositorio;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaResumo;
import dev.pedrocaldas.receitopedia.dominio.receitas.AutorReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.DadosReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.Receita;
import dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.usuarios.UsuarioJpa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;

@Repository
@Transactional(readOnly = true)
public class ReceitaRepositorioJpa implements ReceitaRepositorio {
    @PersistenceContext
    private EntityManager entidades;

    @Override
    @Transactional
    public Receita adicionar(Receita receita) {
        ReceitaJpa registro = new ReceitaJpa(receita, entidades.getReference(UsuarioJpa.class, receita.dono().id()));
        entidades.persist(registro);
        entidades.flush();
        return registro.paraDominio();
    }

    @Override
    public List<ReceitaResumo> listar() {
        return entidades.createQuery("""
                select new dev.pedrocaldas.receitopedia.aplicacao.receitas.ReceitaResumo(
                    receita.id, receita.titulo, receita.descricao)
                from ReceitaJpa receita
                order by lower(receita.titulo), receita.id
                """, ReceitaResumo.class).getResultList();
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Optional<Receita> encontrar(UUID id) {
        return Optional.ofNullable(entidades.find(ReceitaJpa.class, id)).map(ReceitaJpa::paraDominio);
    }

    @Override
    @Transactional
    public Receita editar(UUID id, long versao, UUID usuarioId, DadosReceita dados) {
        ReceitaJpa registro = carregarNaVersao(id, versao, usuarioId, LockModeType.NONE);
        registro.preencher(dados);
        confirmarAlteracao();
        return registro.paraDominio();
    }

    @Override
    @Transactional
    public void remover(UUID id, long versao, UUID usuarioId) {
        entidades.remove(carregarNaVersao(id, versao, usuarioId, LockModeType.PESSIMISTIC_WRITE));
        confirmarAlteracao();
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
