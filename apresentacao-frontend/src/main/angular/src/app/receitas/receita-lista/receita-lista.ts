import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ReceitaApi, ReceitaResumo } from '../receita-api';

@Component({
  selector: 'app-receita-lista',
  imports: [RouterLink],
  templateUrl: './receita-lista.html',
})
export class ReceitaLista implements OnInit {
  private readonly api = inject(ReceitaApi);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly minhas = inject(ActivatedRoute).snapshot.data['minhas'] === true;
  protected readonly receitas = signal<ReceitaResumo[]>([]);
  protected readonly carregando = signal(true);
  protected readonly falha = signal(false);

  ngOnInit() {
    this.carregar();
  }

  protected carregar() {
    this.carregando.set(true);
    this.falha.set(false);
    const consulta = this.minhas ? this.api.listarMinhas() : this.api.listar();
    consulta.pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.carregando.set(false)),
      )
      .subscribe({
        next: (receitas) => this.receitas.set(receitas),
        error: () => this.falha.set(true),
      });
  }
}
