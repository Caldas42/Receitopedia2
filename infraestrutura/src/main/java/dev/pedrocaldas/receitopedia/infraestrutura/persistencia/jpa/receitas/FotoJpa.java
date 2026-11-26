package dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.receitas;

import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.FotoReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.TipoFoto;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
class FotoJpa {
    @Column(name = "foto_id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private TipoFoto tipo;

    protected FotoJpa() {
    }

    FotoJpa(FotoReceita foto) {
        id = foto.id();
        tipo = foto.tipo();
    }

    FotoReceita paraDominio() {
        return new FotoReceita(id, tipo);
    }
}
