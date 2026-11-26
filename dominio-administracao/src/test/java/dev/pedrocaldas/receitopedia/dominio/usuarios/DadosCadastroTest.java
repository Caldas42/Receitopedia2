package dev.pedrocaldas.receitopedia.dominio.usuarios;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DadosCadastroTest {
    @Test
    void normalizaIdentificadoresEPreservaSenha() {
        var dados = new DadosCadastro("  PESSOA@example.com  ", "  Minha.Conta-1_  ", " Abc123! ");
        assertEquals("pessoa@example.com", dados.email());
        assertEquals("Minha.Conta-1_", dados.nomeUsuario());
        assertEquals(" Abc123! ", dados.senha());
        assertEquals("minha.conta-1_", Usuario.novo(dados.email(), dados.nomeUsuario(), "hash").nomeChave());
    }

    @Test
    void informaCamposInvalidos() {
        var erro = assertThrows(CadastroInvalido.class,
                () -> new DadosCadastro("sem-arroba", "com espaço", "curta"));
        assertEquals(3, erro.erros().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcde12!", "ABCD123!", "ABCDabc!", "ABCD123a"})
    void exigeClassesNaSenha(String senha) {
        var erro = assertThrows(CadastroInvalido.class,
                () -> new DadosCadastro("a@example.com", "nome", senha));
        assertTrue(erro.erros().containsKey("senha"));
    }

    @Test
    void limitaSenhaPorBytes() {
        assertDoesNotThrow(() -> new DadosCadastro("a@example.com", "nome", "Aa1!" + "é".repeat(34)));
        assertThrows(CadastroInvalido.class,
                () -> new DadosCadastro("a@example.com", "nome", "Aa1!" + "é".repeat(35)));
    }
}
