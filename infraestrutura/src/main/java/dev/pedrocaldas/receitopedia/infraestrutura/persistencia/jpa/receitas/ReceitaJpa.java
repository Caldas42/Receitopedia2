package dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.receitas;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.DadosReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.Receita;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "receita")
public class ReceitaJpa {
    @Id
    private UUID id;

    @Version
    private long versao;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @ElementCollection
    @CollectionTable(name = "receita_ingrediente", joinColumns = @JoinColumn(name = "receita_id"))
    @OrderColumn(name = "posicao")
    @Column(name = "texto", nullable = false, length = 300)
    private List<String> ingredientes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "receita_etapa", joinColumns = @JoinColumn(name = "receita_id"))
    @OrderColumn(name = "posicao")
    @Column(name = "texto", nullable = false, length = 1000)
    private List<String> etapas = new ArrayList<>();

    protected ReceitaJpa() {
    }

    ReceitaJpa(Receita receita) {
        id = receita.id();
        preencher(receita.dados());
    }

    void preencher(DadosReceita dados) {
        titulo = dados.titulo();
        descricao = dados.descricao();
        ingredientes.clear();
        ingredientes.addAll(dados.ingredientes());
        etapas.clear();
        etapas.addAll(dados.etapas());
    }

    long versao() {
        return versao;
    }

    Receita paraDominio() {
        return new Receita(id, versao, new DadosReceita(titulo, descricao, ingredientes, etapas));
    }
}
