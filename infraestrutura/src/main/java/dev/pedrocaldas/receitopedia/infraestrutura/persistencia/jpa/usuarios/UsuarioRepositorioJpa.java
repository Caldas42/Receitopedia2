package dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.usuarios;

import java.util.Locale;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.pedrocaldas.receitopedia.aplicacao.usuarios.CadastroDuplicado;
import dev.pedrocaldas.receitopedia.aplicacao.usuarios.UsuarioRepositorio;
import dev.pedrocaldas.receitopedia.dominio.usuarios.Papel;
import dev.pedrocaldas.receitopedia.dominio.usuarios.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class UsuarioRepositorioJpa implements UsuarioRepositorio {
    @PersistenceContext
    private EntityManager entidades;

    @Override
    @Transactional
    public void adicionar(Usuario usuario) {
        try {
            entidades.persist(new UsuarioJpa(usuario));
            entidades.flush();
        } catch (ConstraintViolationException e) {
            String restricao = String.valueOf(e.getConstraintName()).toLowerCase(Locale.ROOT);
            if (restricao.contains("uk_usuario_email")) {
                throw new CadastroDuplicado("email", "Este e-mail já está cadastrado.");
            }
            if (restricao.contains("uk_usuario_nome")) {
                throw new CadastroDuplicado("nomeUsuario", "Este nome de usuário já está em uso.");
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public boolean promoverAdmin(String email) {
        return entidades.createQuery("update UsuarioJpa u set u.papel = :papel where u.email = :email")
                .setParameter("papel", Papel.ADMIN)
                .setParameter("email", email)
                .executeUpdate() != 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> encontrarPorEmail(String email) {
        return entidades.createQuery("select u from UsuarioJpa u where u.email = :email", UsuarioJpa.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst()
                .map(UsuarioJpa::paraDominio);
    }
}
