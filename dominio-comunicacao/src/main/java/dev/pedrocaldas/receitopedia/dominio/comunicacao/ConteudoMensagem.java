package dev.pedrocaldas.receitopedia.dominio.comunicacao;

public record ConteudoMensagem(String texto, int anexos) {
    public ConteudoMensagem {
        texto = texto == null ? "" : texto.strip();
        if (texto.codePointCount(0, texto.length()) > 2000) {
            throw new IllegalArgumentException("A mensagem deve ter até 2000 caracteres.");
        }
        if (anexos < 0 || anexos > 4) {
            throw new IllegalArgumentException("Envie até quatro imagens por mensagem.");
        }
        if (texto.isBlank() && anexos == 0) {
            throw new IllegalArgumentException("Escreva uma mensagem ou anexe uma imagem.");
        }
    }
}
