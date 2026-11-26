import { Component, computed, DestroyRef, effect, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { PessoasApi } from '../../pessoas/pessoas-api';
import { SessaoApi } from '../../contas/sessao-api';
import { caminhoFoto, formatarTempo, mensagemDeErro, Receita, ReceitaApi } from '../receita-api';

import { TagsReceita } from '../categorias/tags-receita';

@Component({
  selector: 'app-receita-detalhe',
  imports: [RouterLink, TagsReceita],
  templateUrl: './receita-detalhe.html',
  styleUrl: './receita-detalhe.css',
})
export class ReceitaDetalhe implements OnInit {
  private readonly api = inject(ReceitaApi);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly pessoas = inject(PessoasApi);
  private toque: { x: number; y: number } | null = null;
  private id = '';

  protected readonly sessao = inject(SessaoApi);
  protected readonly caminhoFoto = caminhoFoto;
  protected readonly formatarTempo = formatarTempo;
  protected readonly receita = signal<Receita | null>(null);
  protected readonly carregando = signal(true);
  protected readonly confirmando = signal(false);
  protected readonly removendo = signal(false);
  protected readonly falha = signal('');
  protected readonly conflito = signal(false);
  protected readonly fotoAmpliada = signal<number | null>(null);
  protected readonly seguindo = signal<boolean | null>(null);
  protected readonly alterandoSeguimento = signal(false);
  protected readonly falhaSeguimento = signal('');
  protected readonly autorSeguido = computed(() => {
    const usuario = this.sessao.usuario();
    const dono = this.receita()?.dono;
    return usuario && dono && dono.id !== usuario.id ? dono.id : null;
  });

  constructor() {
    effect((limpar) => {
      const autor = this.autorSeguido();
      this.seguindo.set(null);
      this.falhaSeguimento.set('');
      if (!autor) {
        return;
      }
      let ativa = true;
      limpar(() => { ativa = false; });
      this.pessoas.segue(autor).then(({ seguindo }) => {
        if (ativa) {
          this.seguindo.set(seguindo);
        }
      }).catch(() => {
        if (ativa) {
          this.falhaSeguimento.set('Não foi possível consultar o seguimento.');
        }
      });
    });
  }

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

  protected async alternarSeguimento() {
    const autor = this.autorSeguido();
    const anterior = this.seguindo();
    if (!autor || anterior === null || this.alterandoSeguimento()) {
      return;
    }
    this.falhaSeguimento.set('');
    this.alterandoSeguimento.set(true);
    this.seguindo.set(!anterior);
    try {
      await this.pessoas.alterar(autor, !anterior);
    } catch (erro) {
      this.seguindo.set(anterior);
      this.falhaSeguimento.set(mensagemDeErro(erro as HttpErrorResponse, 'Não foi possível atualizar o seguimento.'));
    } finally {
      this.alterandoSeguimento.set(false);
    }
  }

  protected ampliarFoto(visor: HTMLDialogElement, posicao: number) {
    this.fotoAmpliada.set(posicao);
    visor.showModal();
  }

  protected navegarFoto(passo: 1 | -1) {
    const posicao = this.fotoAmpliada();
    const total = this.receita()?.fotos.length ?? 0;
    if (posicao !== null && total > 1) {
      this.fotoAmpliada.set((posicao + passo + total) % total);
    }
  }

  protected usarTeclado(evento: KeyboardEvent, visor: HTMLDialogElement) {
    if (evento.key === 'Escape') {
      evento.preventDefault();
      visor.close();
    } else if (evento.key === 'ArrowLeft' || evento.key === 'ArrowRight') {
      evento.preventDefault();
      this.navegarFoto(evento.key === 'ArrowLeft' ? -1 : 1);
    }
  }

  protected iniciarToque(evento: TouchEvent) {
    const ponto = evento.touches.item(0);
    this.toque = ponto && evento.touches.length === 1 ? { x: ponto.clientX, y: ponto.clientY } : null;
  }

  protected concluirToque(evento: TouchEvent) {
    const inicio = this.toque;
    this.toque = null;
    const ponto = evento.changedTouches.item(0);
    if (inicio && ponto) {
      const horizontal = ponto.clientX - inicio.x;
      const vertical = ponto.clientY - inicio.y;
      if (Math.abs(horizontal) >= 45 && Math.abs(horizontal) > Math.abs(vertical)) {
        this.navegarFoto(horizontal > 0 ? -1 : 1);
      }
    }
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
