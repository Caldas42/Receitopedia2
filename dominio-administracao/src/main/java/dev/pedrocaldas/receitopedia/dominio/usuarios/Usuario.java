package dev.pedrocaldas.receitopedia.dominio.usuarios;

import java.util.Locale;
import java.util.UUID;

public record Usuario(UUID id, String email, String nomeUsuario, String senhaHash, Papel papel) {
    public static Usuario novo(String email, String nomeUsuario, String senhaHash) {
        return new Usuario(UUID.randomUUID(), email, nomeUsuario, senhaHash, Papel.USER);
    }

    public String nomeChave() {
        return nomeUsuario.toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "Usuario[" + id + "]";
    }
}
