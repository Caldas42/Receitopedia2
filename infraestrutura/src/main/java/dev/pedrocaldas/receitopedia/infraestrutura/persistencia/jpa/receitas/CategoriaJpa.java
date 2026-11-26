package dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.receitas;

import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.Categoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categoria")
class CategoriaJpa {
    @Id
    private UUID id;

    @Column(nullable = false, length = 30)
    private String nome;

    @Column(nullable = false, length = 60)
    private String nomeChave;

    @Column(nullable = false)
    private long posicao;

    protected CategoriaJpa() {
    }

    CategoriaJpa(Categoria categoria, long posicao) {
        id = categoria.id();
        this.posicao = posicao;
        renomear(categoria);
    }

    void renomear(Categoria categoria) {
        nome = categoria.nome();
        nomeChave = categoria.nomeChave();
    }

    UUID id() {
        return id;
    }

    void posicionar(long posicao) {
        this.posicao = posicao;
    }

    Categoria paraDominio() {
        return new Categoria(id, nome);
    }
}
