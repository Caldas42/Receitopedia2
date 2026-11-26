import { HttpErrorResponse } from '@angular/common/http';
import { AfterViewInit, Component, DestroyRef, ElementRef, inject, signal, viewChildren } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { mensagemDeErro } from '../receitas/receita-api';
import { Pessoa, PessoasApi } from './pessoas-api';

type Lista = 'SEGUINDO' | 'TODOS';

interface EstadoLista {
  pessoas: Pessoa[];
  cursor: string | null;
  carregando: boolean;
  falhou: boolean;
}

const INICIAL: EstadoLista = { pessoas: [], cursor: '', carregando: false, falhou: false };

@Component({
  selector: 'app-pessoas',
  imports: [ReactiveFormsModule],
  templateUrl: './pessoas-pagina.html',
  styleUrl: './pessoas-pagina.css',
})
export class PessoasPagina implements AfterViewInit {
  private readonly api = inject(PessoasApi);
  private readonly destroy = inject(DestroyRef);
  private readonly sentinelas = viewChildren<ElementRef<HTMLDivElement>>('sentinela');
  private geracao = 0;
  private termo = '';

  protected readonly abas: { tipo: Lista; titulo: string }[] = [
    { tipo: 'SEGUINDO', titulo: 'Seguindo' },
    { tipo: 'TODOS', titulo: 'Todos os usuários' },
  ];
  protected readonly busca = new FormControl('', { nonNullable: true });
  protected readonly listas = signal<Record<Lista, EstadoLista>>({ SEGUINDO: INICIAL, TODOS: INICIAL });
  protected readonly alterando = signal<string | null>(null);
  protected readonly erro = signal('');

  ngAfterViewInit() {
    this.busca.valueChanges.pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.destroy))
      .subscribe((texto) => {
        this.termo = texto.trim();
        this.reiniciar();
      });
    const observadores = this.sentinelas().map((ref, indice) => {
      const sentinela = ref.nativeElement;
      const observador = new IntersectionObserver((entradas) => {
        const tipo = this.abas[indice].tipo;
        if (entradas.some((entrada) => entrada.isIntersecting) && !this.listas()[tipo].falhou) {
          void this.carregar(tipo);
        }
      }, { root: sentinela.parentElement, rootMargin: '0px 0px 80px 0px' });
      observador.observe(sentinela);
      return observador;
    });
    this.destroy.onDestroy(() => {
      this.geracao++;
      observadores.forEach((observador) => observador.disconnect());
    });
    // O observador inicia as duas consultas quando as listas entram na tela.
  }

  protected async carregar(tipo: Lista) {
    const lista = this.listas()[tipo];
    if (lista.cursor === null || lista.carregando || this.alterando()) {
      return;
    }
    const geracao = this.geracao;
    this.atualizar(tipo, { ...lista, carregando: true, falhou: false });
    try {
      const pagina = await this.api.listar(tipo, this.termo, lista.cursor);
      if (geracao === this.geracao) {
        this.atualizar(tipo, { pessoas: lista.cursor === '' ? pagina.pessoas : [...lista.pessoas, ...pagina.pessoas], cursor: pagina.proximo,
          carregando: false, falhou: false });
      }
    } catch {
      if (geracao === this.geracao) {
        this.atualizar(tipo, { ...lista, carregando: false, falhou: true });
      }
    }
  }

  protected async alterar(tipo: Lista, pessoa: Pessoa) {
    if (this.alterando()) {
      return;
    }
    const seguir = tipo === 'TODOS';
    const destino = seguir ? 'SEGUINDO' : 'TODOS';
    this.geracao++;
    this.alterando.set(pessoa.id);
    this.erro.set('');
    const anteriores = this.listas();
    const termoAnterior = this.termo;
    const novaLista = [...anteriores[destino].pessoas, pessoa]
      .sort((a, b) => a.cursor.localeCompare(b.cursor));
    this.listas.set({
      ...anteriores,
      [tipo]: { ...anteriores[tipo], pessoas: anteriores[tipo].pessoas.filter(({ id }) => id !== pessoa.id) },
      [destino]: { ...anteriores[destino], pessoas: novaLista },
    });
    try {
      await this.api.alterar(pessoa.id, seguir);
    } catch (erro) {
      if (this.termo === termoAnterior) {
        this.listas.set(anteriores);
      }
      this.erro.set(mensagemDeErro(erro as HttpErrorResponse, 'Não foi possível atualizar o seguimento.'));
    } finally {
      this.alterando.set(null);
      if (!this.destroy.destroyed) {
        this.reiniciar(true);
      }
    }
  }

  private reiniciar(preservar = false) {
    this.geracao++;
    this.listas.update((atuais) => ({
      SEGUINDO: { ...INICIAL, pessoas: preservar ? atuais.SEGUINDO.pessoas : [] },
      TODOS: { ...INICIAL, pessoas: preservar ? atuais.TODOS.pessoas : [] },
    }));
    this.abas.forEach(({ tipo }) => void this.carregar(tipo));
  }

  private atualizar(tipo: Lista, estado: EstadoLista) {
    this.listas.update((listas) => ({ ...listas, [tipo]: estado }));
  }
}
