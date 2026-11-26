package dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.receitas;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalogo_categorias")
class CatalogoCategoriasJpa {
    @Id
    private int id;

    protected CatalogoCategoriasJpa() {
    }

    static void bloquear(EntityManager entidades, LockModeType modo) {
        entidades.find(CatalogoCategoriasJpa.class, 1, modo);
    }
}
