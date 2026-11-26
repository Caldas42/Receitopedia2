package dev.pedrocaldas.receitopedia.aplicacao.usuarios;

public class CadastroDuplicado extends RuntimeException {
    private final String campo;

    public CadastroDuplicado(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
