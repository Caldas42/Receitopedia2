import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { SessaoApi } from './contas/sessao-api';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterOutlet],
  templateUrl: './app.html',
})
export class App {
  private readonly router = inject(Router);
  private readonly destroy = inject(DestroyRef);
  protected readonly sessao = inject(SessaoApi);
  protected readonly falhaAoSair = signal(false);

  constructor() {
    this.sessao.consultar().pipe(takeUntilDestroyed(this.destroy)).subscribe({ error: () => {} });
  }

  protected sair() {
    this.falhaAoSair.set(false);
    this.sessao.sair().pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: () => void this.router.navigate(['/receitas']),
      error: () => this.falhaAoSair.set(true),
    });
  }
}
