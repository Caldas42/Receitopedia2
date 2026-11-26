package dev.pedrocaldas.receitopedia.dominio.usuarios;

import java.util.Map;

public class CadastroInvalido extends RuntimeException {
    private final Map<String, String> erros;

    public CadastroInvalido(Map<String, String> erros) {
        super("Confira os campos do cadastro.");
        this.erros = Map.copyOf(erros);
    }

    public Map<String, String> erros() {
        return erros;
    }
}
