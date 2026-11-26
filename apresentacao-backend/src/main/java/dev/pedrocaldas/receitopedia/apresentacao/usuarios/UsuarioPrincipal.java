package dev.pedrocaldas.receitopedia.apresentacao.usuarios;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import dev.pedrocaldas.receitopedia.dominio.usuarios.Usuario;

public class UsuarioPrincipal extends User {
    private final UUID id;
    private final String nomeUsuario;

    UsuarioPrincipal(Usuario usuario) {
        super(usuario.email(), usuario.senhaHash(),
                List.of(new SimpleGrantedAuthority(
                        "ROLE_" + usuario.papel().name())));
        id = usuario.id();
        nomeUsuario = usuario.nomeUsuario();
    }

    public UUID id() {
        return id;
    }

    public String nomeUsuario() {
        return nomeUsuario;
    }
}
