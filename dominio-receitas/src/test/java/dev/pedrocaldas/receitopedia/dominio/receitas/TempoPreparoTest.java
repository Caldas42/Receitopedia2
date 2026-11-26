package dev.pedrocaldas.receitopedia.dominio.receitas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TempoPreparoTest {
    @Test
    void aceitaHorasOuMinutosSeparadamenteEDistingueAusenciaDeZero() {
        assertNull(TempoPreparo.informado(null, null));
        assertEquals(0, TempoPreparo.informado(0, null).minutosTotais());
        assertEquals(120, TempoPreparo.informado(2, null).minutosTotais());
        assertEquals(45, TempoPreparo.informado(null, 45).minutosTotais());
        assertEquals(90, TempoPreparo.informado(1, 30).minutosTotais());
    }

    @Test
    void rejeitaValoresForaDosLimites() {
        assertThrows(IllegalArgumentException.class, () -> TempoPreparo.informado(-1, null));
        assertThrows(IllegalArgumentException.class, () -> TempoPreparo.informado(100, 0));
        assertThrows(IllegalArgumentException.class, () -> TempoPreparo.informado(null, -1));
        assertThrows(IllegalArgumentException.class, () -> TempoPreparo.informado(1, 60));
    }
}
