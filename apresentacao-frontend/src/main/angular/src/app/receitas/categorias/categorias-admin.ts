import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
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
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly carregando = signal(true);
  protected readonly salvando = signal(false);
  protected readonly falhaLista = signal(false);
  protected readonly falhaCadastro = signal('');

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
    if (this.salvando()) {
      return;
    }
    this.salvando.set(true);
    this.falhaCadastro.set('');
    this.api.adicionar(this.nome.value).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.salvando.set(false)),
    ).subscribe({
      next: () => {
        this.nome.reset();
        this.carregar();
      },
      error: (erro) => this.falhaCadastro.set(mensagemDeErro(erro, 'Não foi possível adicionar a categoria.')),
    });
  }
}
