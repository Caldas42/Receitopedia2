import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { finalize, Observable } from 'rxjs';
import { mensagemDeErro } from '../receita-api';
import { Categoria, CategoriaApi } from './categoria-api';

@Component({
  selector: 'app-categorias-admin',
  imports: [ReactiveFormsModule],
  templateUrl: './categorias-admin.html',
})
export class CategoriasAdmin implements OnInit {
  private readonly api = inject(CategoriaApi);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly nome = new FormControl('', { nonNullable: true });
  protected readonly novoNome = new FormControl('', { nonNullable: true });
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly carregando = signal(true);
  protected readonly salvando = signal(false);
  protected readonly falhaLista = signal(false);
  protected readonly falhaCadastro = signal('');
  protected readonly falhaAcao = signal('');
  protected readonly feedback = signal('');
  protected readonly editando = signal<string | null>(null);
  protected readonly confirmandoRemocao = signal<string | null>(null);

  ngOnInit() {
    this.carregar();
  }

  protected carregar() {
    this.carregando.set(true);
    this.falhaLista.set(false);
    this.api.listar().pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carregando.set(false)),
    ).subscribe({
      next: (categorias) => this.categorias.set(categorias),
      error: () => this.falhaLista.set(true),
    });
  }

  protected adicionar() {
    this.falhaCadastro.set('');
    this.executar(this.api.adicionar(this.nome.value), () => {
      this.nome.reset();
      this.feedback.set('Categoria adicionada.');
      this.carregar();
    }, (erro) => {
      this.falhaCadastro.set(mensagemDeErro(erro, 'Não foi possível adicionar a categoria.'));
    });
  }

  protected iniciarEdicao(categoria: Categoria) {
    this.confirmandoRemocao.set(null);
    this.falhaAcao.set('');
    this.editando.set(categoria.id);
    this.novoNome.setValue(categoria.nome);
  }

  protected iniciarRemocao(categoria: Categoria) {
    this.editando.set(null);
    this.falhaAcao.set('');
    this.confirmandoRemocao.set(categoria.id);
  }

  protected cancelarAcao() {
    this.editando.set(null);
    this.confirmandoRemocao.set(null);
    this.falhaAcao.set('');
  }

  protected editar(categoria: Categoria) {
    this.executar(this.api.editar(categoria.id, this.novoNome.value), (atualizada) => {
      this.categorias.update((lista) => lista.map((item) => item.id === atualizada.id ? atualizada : item));
      this.cancelarAcao();
      this.feedback.set('Categoria atualizada.');
    }, (erro) => this.falhaAcao.set(mensagemDeErro(erro, 'Não foi possível editar a categoria.')));
  }

  protected remover(categoria: Categoria) {
    this.executar(this.api.remover(categoria.id), () => {
      this.categorias.update((lista) => lista.filter((item) => item.id !== categoria.id));
      this.cancelarAcao();
      this.feedback.set('Categoria removida.');
    }, (erro) => this.falhaAcao.set(mensagemDeErro(erro, 'Não foi possível remover a categoria.')));
  }

  protected mover(categoria: Categoria, deslocamento: -1 | 1) {
    const ids = this.categorias().map(({ id }) => id);
    const indice = ids.indexOf(categoria.id);
    const destino = indice + deslocamento;
    if (indice < 0 || destino < 0 || destino >= ids.length) {
      return;
    }
    [ids[indice], ids[destino]] = [ids[destino], ids[indice]];
    this.executar(this.api.reordenar(ids), (ordem) => {
      this.categorias.set(ordem);
      this.feedback.set(categoria.nome + ' está na posição ' + String(destino + 1) + '.');
    }, (erro) => {
      this.falhaAcao.set(mensagemDeErro(erro, 'Não foi possível salvar a ordem das categorias.'));
      if (erro.status === 409) {
        this.carregar();
      }
    });
  }

  private executar<T>(requisicao: Observable<T>, sucesso: (resultado: T) => void,
      falha: (erro: HttpErrorResponse) => void) {
    if (this.salvando() || this.carregando()) {
      return;
    }
    this.salvando.set(true);
    this.falhaAcao.set('');
    this.feedback.set('');
    requisicao.pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.salvando.set(false)),
    ).subscribe({ next: sucesso, error: falha });
  }
}
