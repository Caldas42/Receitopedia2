package dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.receitas;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import dev.pedrocaldas.receitopedia.dominio.receitas.AutorReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.DadosReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.FotoReceita;
import dev.pedrocaldas.receitopedia.dominio.receitas.Receita;
import dev.pedrocaldas.receitopedia.dominio.receitas.TempoPreparo;
import dev.pedrocaldas.receitopedia.dominio.receitas.TextosReceitas;
import dev.pedrocaldas.receitopedia.infraestrutura.persistencia.jpa.usuarios.UsuarioJpa;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "receita")
public class ReceitaJpa {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dono_id", updatable = false)
    private UsuarioJpa dono;

    @Version
    private long versao;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 240)
    private String tituloChave;

    @Column(nullable = false, length = 1000)
    private String descricao;

    private Integer tempoMinutos;

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

    @ElementCollection
    @CollectionTable(name = "receita_foto", joinColumns = @JoinColumn(name = "receita_id"))
    @OrderColumn(name = "posicao")
    private List<FotoJpa> fotos = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "receita_categoria", joinColumns = @JoinColumn(name = "receita_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_id"))
    @OrderBy("nomeChave")
    private Set<CategoriaJpa> categorias = new LinkedHashSet<>();

    protected ReceitaJpa() {
    }

    ReceitaJpa(Receita receita, UsuarioJpa dono) {
        id = receita.id();
        this.dono = dono;
        preencher(receita.dados());
    }

    void preencher(DadosReceita dados) {
        titulo = dados.titulo();
        tituloChave = TextosReceitas.normalizar(titulo);
        descricao = dados.descricao();
        tempoMinutos = dados.tempoPreparo() == null ? null : dados.tempoPreparo().minutosTotais();
        ingredientes.clear();
        ingredientes.addAll(dados.ingredientes());
        etapas.clear();
        etapas.addAll(dados.etapas());
    }

    long versao() {
        return versao;
    }

    AutorReceita autor() {
        return dono == null ? null : new AutorReceita(dono.id(), dono.nomeUsuario());
    }

    List<FotoReceita> fotos() {
        return fotos.stream().map(FotoJpa::paraDominio).toList();
    }

    void atualizarFotos(List<FotoReceita> novas) {
        if (!fotos().equals(novas)) {
            fotos.clear();
            novas.stream().map(FotoJpa::new).forEach(fotos::add);
        }
    }

    void atualizarCategorias(List<CategoriaJpa> novas) {
        if (!categorias.equals(new LinkedHashSet<>(novas))) {
            categorias.clear();
            categorias.addAll(novas);
        }
    }

    Receita paraDominio() {
        var tempo = tempoMinutos == null ? null : new TempoPreparo(tempoMinutos);
        return new Receita(id, versao, new DadosReceita(titulo, descricao, ingredientes, etapas, tempo), autor(), fotos(),
                categorias.stream().map(CategoriaJpa::paraDominio).toList());
    }
}
