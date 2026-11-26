package dev.pedrocaldas.receitopedia.aplicacao.usuarios;

import java.util.Optional;
import dev.pedrocaldas.receitopedia.dominio.usuarios.Usuario;

public interface UsuarioRepositorio {
    void adicionar(Usuario usuario);

    Optional<Usuario> encontrarPorEmail(String email);

    boolean promoverAdmin(String email);
}
