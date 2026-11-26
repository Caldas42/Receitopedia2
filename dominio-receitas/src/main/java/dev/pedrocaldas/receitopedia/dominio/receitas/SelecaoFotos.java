package dev.pedrocaldas.receitopedia.dominio.receitas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SelecaoFotos(List<UUID> mantidas, List<FotoReceita> novas, Integer principal) {
    private static final int LIMITE = 5;

    public SelecaoFotos {
        if (mantidas.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Informe identificadores válidos para as fotos existentes.");
        }
        mantidas = List.copyOf(mantidas);
        novas = List.copyOf(novas);
        int quantidade = mantidas.size() + novas.size();
        validarQuantidade(quantidade);
        if (principal != null && (principal < 0 || principal >= quantidade)) {
            throw new IllegalArgumentException("Selecione a foto principal entre as fotos da receita.");
        }
    }

    public static void validarQuantidade(int quantidade) {
        if (quantidade > LIMITE) {
            throw new IllegalArgumentException("Cada receita pode ter até 5 fotos.");
        }
    }

    public List<FotoReceita> aplicar(List<FotoReceita> atuais) {
        var restantes = new HashMap<UUID, FotoReceita>();
        atuais.forEach(foto -> restantes.put(foto.id(), foto));

        var fotos = new ArrayList<FotoReceita>();
        for (UUID id : mantidas) {
            FotoReceita foto = restantes.remove(id);
            if (foto == null) {
                throw new IllegalArgumentException("Selecione fotos distintas pertencentes à receita.");
            }
            fotos.add(foto);
        }
        fotos.addAll(novas);
        if (principal != null && principal != 0) {
            fotos.add(0, fotos.remove(principal.intValue()));
        }
        return List.copyOf(fotos);
    }
}
