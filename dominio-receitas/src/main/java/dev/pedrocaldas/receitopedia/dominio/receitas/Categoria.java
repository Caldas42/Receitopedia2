package dev.pedrocaldas.receitopedia.dominio.receitas;

import java.util.UUID;

public record Categoria(UUID id, String nome) {
    public Categoria {
        nome = TextosReceitas.ajustarEspacos(nome);
        int tamanho = nome.codePointCount(0, nome.length());
        if (tamanho < 2 || tamanho > 30 || TextosReceitas.normalizar(nome).isBlank()) {
            throw new IllegalArgumentException("O nome da categoria deve ter entre 2 e 30 caracteres.");
        }
    }

    public static Categoria nova(String nome) {
        return new Categoria(UUID.randomUUID(), nome);
    }

    public String nomeChave() {
        return TextosReceitas.normalizar(nome);
    }
}
