package dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.usuarios;

import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.usuarios.Papel;
import dev.pedrocaldas.receitopedia.dominio.usuarios.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class UsuarioJpa {
    @Id
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 20)
    private String nomeUsuario;

    @Column(nullable = false, length = 20)
    private String nomeChave;

    @Column(nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Papel papel;

    protected UsuarioJpa() {
    }

    UsuarioJpa(Usuario usuario) {
        id = usuario.id();
        email = usuario.email();
        nomeUsuario = usuario.nomeUsuario();
        nomeChave = usuario.nomeChave();
        senhaHash = usuario.senhaHash();
        papel = usuario.papel();
    }

    public UUID id() {
        return id;
    }

    public String nomeUsuario() {
        return nomeUsuario;
    }

    Usuario paraDominio() {
        return new Usuario(id, email, nomeUsuario, senhaHash, papel);
    }
}
