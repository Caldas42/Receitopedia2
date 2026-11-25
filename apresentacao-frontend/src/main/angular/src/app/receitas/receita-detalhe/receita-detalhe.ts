import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { mensagemDeErro, Receita, ReceitaApi } from '../receita-api';

@Component({
  selector: 'app-receita-detalhe',
  imports: [RouterLink],
  templateUrl: './receita-detalhe.html',
})
export class ReceitaDetalhe implements OnInit {
  private readonly api = inject(ReceitaApi);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private id = '';

  protected readonly receita = signal<Receita | null>(null);
  protected readonly carregando = signal(true);
  protected readonly confirmando = signal(false);
  protected readonly removendo = signal(false);
  protected readonly falha = signal('');
  protected readonly conflito = signal(false);

  ngOnInit() {
    this.id = this.rota.snapshot.paramMap.get('id') ?? '';
    this.carregar();
  }

  protected carregar() {
    this.carregando.set(true);
    this.falha.set('');
    this.conflito.set(false);
    this.confirmando.set(false);
    this.api
      .visualizar(this.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.carregando.set(false)),
      )
      .subscribe({
        next: (receita) => this.receita.set(receita),
        error: (erro) => {
          this.receita.set(null);
          this.falha.set(mensagemDeErro(erro, 'Não foi possível carregar a receita.'));
        },
      });
  }

  protected remover(receita: Receita) {
    if (this.removendo()) {
      return;
    }
    this.removendo.set(true);
    this.falha.set('');
    this.api
      .remover(receita.id, receita.versao)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.removendo.set(false)),
      )
      .subscribe({
        next: () => void this.router.navigate(['/receitas']),
        error: (erro) => {
          this.confirmando.set(false);
          this.conflito.set(erro.status === 409);
          if (erro.status === 404) {
            this.receita.set(null);
          }
          this.falha.set(mensagemDeErro(erro, 'Não foi possível remover a receita.'));
        },
      });
  }
}
