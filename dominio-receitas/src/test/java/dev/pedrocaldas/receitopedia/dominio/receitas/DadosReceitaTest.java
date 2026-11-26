package dev.pedrocaldas.receitopedia.dominio.receitas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class DadosReceitaTest {
    @Test
    void exigeTituloNaoVazioDentroDoLimiteEDescartaEspacosNasPontas() {
        assertEquals("Café", receita("  Café  ").titulo());
        assertEquals(120, receita("a".repeat(120)).titulo().length());
        assertThrows(IllegalArgumentException.class, () -> receita(null));
        assertThrows(IllegalArgumentException.class, () -> receita(" \t "));
        assertThrows(IllegalArgumentException.class, () -> receita("a".repeat(121)));
    }

    private DadosReceita receita(String titulo) {
        return new DadosReceita(titulo, null, List.of("Ingrediente"), List.of("Preparar"), null);
    }
}
