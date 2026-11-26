package dev.pedrocaldas.receitopedia.dominio.receitas;

import java.util.UUID;

public record AutorReceita(UUID id, String nomeUsuario) {
    public static void exigirAutoria(AutorReceita autor, UUID usuarioId) {
        if (autor == null || !autor.id().equals(usuarioId)) {
            throw new ReceitaAcessoNegado();
        }
    }
}
