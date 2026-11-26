import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { finalize, Observable } from 'rxjs';
import { mensagemDeErro } from '../receita-api';
import { Categoria, CategoriaApi } from './categoria-api';

@Component({
  selector: 'app-categorias-admin',
  imports: [ReactiveFormsModule, CdkDrag, CdkDragHandle, CdkDropList],
  styleUrl: './categorias-admin.css',
  templateUrl: './categorias-admin.html',
})
export class CategoriasAdmin implements OnInit {
  private readonly api = inject(CategoriaApi);
  private readonly destroyRef = inject(DestroyRef);
  private versao = 0;
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
    this.api.catalogo().pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carregando.set(false)),
    ).subscribe({
      next: ({ categorias, versao }) => {
        this.categorias.set(categorias);
        this.versao = versao;
      },
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
    this.executar(this.api.editar(categoria.id, this.novoNome.value), () => {
      this.carregar();
      this.cancelarAcao();
      this.feedback.set('Categoria atualizada.');
    }, (erro) => this.falhaAcao.set(mensagemDeErro(erro, 'Não foi possível editar a categoria.')));
  }

  protected remover(categoria: Categoria) {
    this.executar(this.api.remover(categoria.id), () => {
      this.carregar();
      this.cancelarAcao();
      this.feedback.set('Categoria removida.');
    }, (erro) => this.falhaAcao.set(mensagemDeErro(erro, 'Não foi possível remover a categoria.')));
  }

  protected soltar(evento: CdkDragDrop<Categoria[]>) {
    this.reordenar(evento.previousIndex, evento.currentIndex);
  }

  protected moverComTeclado(evento: Event, indice: number, passo: -1 | 1) {
    evento.preventDefault();
    this.reordenar(indice, indice + passo);
  }

  private reordenar(origem: number, destino: number) {
    const antes = this.categorias();
    if (this.carregando() || this.salvando() || this.editando() || this.confirmandoRemocao()
        || origem === destino || destino < 0 || destino >= antes.length) {
      return;
    }
    const depois = [...antes];
    moveItemInArray(depois, origem, destino);
    this.categorias.set(depois);
    this.executar(this.api.reordenar(depois.map(({ id }) => id), this.versao), ({ categorias, versao }) => {
      this.categorias.set(categorias);
      this.versao = versao;
      this.feedback.set(depois[destino].nome + ' está na posição ' + String(destino + 1) + '.');
    }, (erro) => {
      this.categorias.set(antes);
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
