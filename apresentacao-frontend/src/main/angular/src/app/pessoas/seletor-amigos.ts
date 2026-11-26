import { Component, DestroyRef, ElementRef, inject, input, model, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, Subject } from 'rxjs';
import { Pessoa, PessoasApi } from './pessoas-api';

@Component({
  selector: 'app-seletor-amigos',
  templateUrl: './seletor-amigos.html',
})
export class SeletorAmigos {
  private readonly api = inject(PessoasApi);
  private readonly destroy = inject(DestroyRef);
  private readonly entrada = viewChild<ElementRef<HTMLInputElement>>('entrada');
  private readonly consultas = new Subject<string>();
  private geracao = 0;
  readonly membros = input<readonly string[]>([]);
  readonly limite = input(255);
  readonly desativado = input(false);
  readonly selecionados = model<Pessoa[]>([]);
  protected readonly busca = signal('');
  protected readonly resultados = signal<Pessoa[]>([]);
  protected readonly proximo = signal<string | null>(null);
  protected readonly carregando = signal(false);
  protected readonly falha = signal(false);

  constructor() {
    this.consultas.pipe(debounceTime(200), takeUntilDestroyed(this.destroy)).subscribe((texto) => {
      if (texto) {
        void this.consultar('');
      }
    });
    this.destroy.onDestroy(() => this.geracao++);
  }

  protected digitar(texto: string) {
    this.geracao++;
    this.busca.set(texto);
    this.resultados.set([]);
    this.proximo.set(null);
    this.falha.set(false);
    this.carregando.set(!!texto.trim());
    this.consultas.next(texto.trim());
  }

  protected async consultar(cursor: string) {
    const geracao = this.geracao;
    this.carregando.set(true);
    this.falha.set(false);
    try {
      const pagina = await this.api.listar('AMIGOS', this.busca().trim(), cursor);
      if (geracao === this.geracao) {
        this.resultados.update((atuais) => cursor ? [...atuais, ...pagina.pessoas] : pagina.pessoas);
        this.proximo.set(pagina.proximo);
      }
    } catch {
      if (geracao === this.geracao) {
        this.falha.set(true);
      }
    } finally {
      if (geracao === this.geracao) {
        this.carregando.set(false);
      }
    }
  }

  protected disponiveis() {
    const excluidos = new Set([...this.membros(), ...this.selecionados().map((pessoa) => pessoa.id)]);
    return this.resultados().filter((pessoa) => !excluidos.has(pessoa.id));
  }

  protected escolher(pessoa: Pessoa) {
    if (!this.desativado() && this.selecionados().length < this.limite()) {
      this.selecionados.update((atuais) => [...atuais, pessoa]);
      this.digitar('');
      this.entrada()?.nativeElement.focus();
    }
  }

  protected retirar(id: string) {
    this.selecionados.update((atuais) => atuais.filter((pessoa) => pessoa.id !== id));
  }
}
