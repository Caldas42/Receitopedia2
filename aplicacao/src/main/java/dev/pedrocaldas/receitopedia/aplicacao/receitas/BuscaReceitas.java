package dev.pedrocaldas.receitopedia.aplicacao.receitas;

import java.util.List;

import dev.pedrocaldas.receitopedia.dominio.receitas.TextosReceitas;

public record BuscaReceitas(String termo, int pagina) {
    public static final int POR_PAGINA = 12;

    public BuscaReceitas {
        termo = TextosReceitas.ajustarEspacos(termo);
        if (termo.codePointCount(0, termo.length()) > 100) {
            throw new IllegalArgumentException("A busca deve ter até 100 caracteres.");
        }
        if (pagina < 0 || pagina > Integer.MAX_VALUE / POR_PAGINA) {
            throw new IllegalArgumentException("Informe um número de página válido.");
        }
        termo = TextosReceitas.ajustarEspacos(TextosReceitas.normalizar(termo));
    }

    public List<String> palavras() {
        return termo.isBlank() ? List.of() : List.of(termo.split(" "));
    }
}
