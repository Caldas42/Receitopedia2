package dev.pedrocaldas.receitopedia.dominio.receitas;

import java.util.List;

public record DadosReceita(String titulo, String descricao, List<String> ingredientes,
        List<String> etapas) {
    public DadosReceita {
        titulo = titulo == null ? "" : titulo.strip();
        if (!titulo.isEmpty()) {
            titulo = textoObrigatorio(titulo, 120, "Título");
        }
        descricao = descricao == null ? "" : descricao.strip();
        if (descricao.length() > 1000) {
            throw new IllegalArgumentException("A descrição deve ter até 1000 caracteres.");
        }
        ingredientes = itensObrigatorios(ingredientes, 300, "ingrediente", "ingredientes");
        etapas = itensObrigatorios(etapas, 1000, "etapa", "etapas");
    }

    private static List<String> itensObrigatorios(List<String> itens, int limite, String singular,
            String plural) {
        if (itens == null || itens.isEmpty() || itens.size() > 50) {
            throw new IllegalArgumentException("Informe entre 1 e 50 " + plural + ".");
        }
        return itens.stream().map(item -> textoObrigatorio(item, limite, "Cada " + singular)).toList();
    }

    private static String textoObrigatorio(String valor, int limite, String campo) {
        if (valor == null || valor.isBlank() || valor.strip().length() > limite) {
            throw new IllegalArgumentException(campo + " deve ter entre 1 e " + limite + " caracteres.");
        }
        String texto = valor.strip();
        if (texto.lines().count() != 1) {
            throw new IllegalArgumentException(campo + " deve ocupar uma única linha.");
        }
        return texto;
    }
}
