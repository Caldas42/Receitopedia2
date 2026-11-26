import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, combineLatest, of, startWith, Subject, switchMap } from 'rxjs';
import { TagsReceita } from '../categorias/tags-receita';
import { caminhoFoto, formatarTempo, mensagemDeErro, ReceitaApi, ReceitaResumo } from '../receita-api';

@Component({
  selector: 'app-receita-lista',
  imports: [RouterLink, ReactiveFormsModule, TagsReceita],
  templateUrl: './receita-lista.html',
})
export class ReceitaLista implements OnInit {
  private readonly api = inject(ReceitaApi);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly recarregar = new Subject<void>();
  protected readonly minhas = this.rota.snapshot.data['minhas'] === true;
  protected readonly destino = this.minhas ? '/minhas-receitas' : '/receitas';
  protected readonly caminhoFoto = caminhoFoto;
  protected readonly formatarTempo = formatarTempo;
  protected readonly receitas = signal<ReceitaResumo[]>([]);
  protected readonly carregando = signal(true);
  protected readonly falha = signal('');
  protected readonly busca = new FormControl('', { nonNullable: true });
  protected readonly termo = signal('');
  protected readonly pagina = signal(0);
  protected readonly temProxima = signal(false);

  ngOnInit() {
    combineLatest([this.rota.queryParamMap, this.recarregar.pipe(startWith(undefined))]).pipe(
      switchMap(([params]) => {
        const busca = (params.get('busca') ?? '').trim();
        const pagina = Number(params.get('pagina') ?? '0');
        this.busca.setValue(busca);
        this.termo.set(busca);
        this.pagina.set(Number.isSafeInteger(pagina) && pagina >= 0 ? pagina : 0);
        this.carregando.set(true);
        this.falha.set('');
        return this.api.listar(this.minhas, busca, this.pagina()).pipe(
          catchError((erro) => {
            this.falha.set(mensagemDeErro(erro, 'Não foi possível carregar as receitas.'));
            return of(null);
          }),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((resultado) => {
      this.receitas.set(resultado?.receitas ?? []);
      this.temProxima.set(resultado?.temProxima ?? false);
      this.carregando.set(false);
    });
  }

  protected carregar() {
    this.recarregar.next();
  }

  protected pesquisar() {
    void this.router.navigate([this.destino], { queryParams: { busca: this.busca.value.trim() || null } });
  }

  protected limpar() {
    this.busca.reset();
    this.pesquisar();
  }
}
