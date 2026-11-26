import { Component, DestroyRef, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { SessaoApi } from '../../contas/sessao-api';
import { caminhoFoto, DadosReceita, FotosParaSalvar, mensagemDeErro, ReceitaApi } from '../receita-api';

type FotoFormulario = { url: string; original: string } | { url: string; arquivo: File };

@Component({
  selector: 'app-receita-formulario',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './receita-formulario.html',
})
export class ReceitaFormulario implements OnInit, OnDestroy {
  private readonly sessao = inject(SessaoApi);
  private readonly api = inject(ReceitaApi);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly id = this.rota.snapshot.paramMap.get('id');
  private versao: number | null = null;

  protected readonly carregando = signal(!!this.id);
  protected readonly salvando = signal(false);
  protected readonly pronto = signal(!this.id);
  protected readonly falha = signal('');
  protected readonly fotos = signal<FotoFormulario[]>([]);
  protected readonly fotoPrincipal = signal<FotoFormulario | null>(null);
  protected readonly erroFotos = signal('');
  protected readonly conflito = signal(false);
  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    titulo: ['', Validators.maxLength(120)],
    descricao: ['', Validators.maxLength(1000)],
    ingredientes: ['', Validators.required],
    etapas: ['', Validators.required],
    horas: new FormControl<number | null>(null),
    minutos: new FormControl<number | null>(null),
  });

  ngOnInit() {
    if (!this.id) {
      return;
    }
    this.api
      .visualizar(this.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.carregando.set(false)),
      )
      .subscribe({
        next: ({ id, dados, versao, dono, fotos }) => {
          if (!dono || dono.id !== this.sessao.usuario()?.id) {
            this.falha.set('Somente o dono pode editar esta receita.');
            return;
          }
          this.versao = versao;
          const existentes = fotos.map(({ id: fotoId }) => ({ original: fotoId, url: caminhoFoto(id, fotoId) }));
          this.fotos.set(existentes);
          this.fotoPrincipal.set(existentes[0] ?? null);
          this.formulario.setValue({
            titulo: dados.titulo,
            descricao: dados.descricao,
            ingredientes: dados.ingredientes.join('\n'),
            etapas: dados.etapas.join('\n'),
            horas: dados.tempoPreparo ? Math.floor(dados.tempoPreparo.minutosTotais / 60) : null,
            minutos: dados.tempoPreparo ? dados.tempoPreparo.minutosTotais % 60 : null,
          });
          this.pronto.set(true);
        },
        error: (erro) => this.falha.set(mensagemDeErro(erro, 'Não foi possível carregar a receita.')),
      });
  }

  ngOnDestroy() {
    for (const foto of this.fotos()) {
      if ('arquivo' in foto) {
        URL.revokeObjectURL(foto.url);
      }
    }
  }

  protected selecionarFotos(evento: Event) {
    const entrada = evento.target as HTMLInputElement;
    const arquivos = Array.from(entrada.files ?? []);
    entrada.value = '';
    this.erroFotos.set('');

    if (arquivos.length + this.fotos().length > 5) {
      this.erroFotos.set('Cada receita pode ter até 5 fotos.');
      return;
    }
    if (arquivos.some((arquivo) => arquivo.size > 5 * 1024 * 1024)) {
      this.erroFotos.set('Cada foto deve ter no máximo 5 MB.');
      return;
    }

    const novas = arquivos.map((arquivo) => ({ arquivo, url: URL.createObjectURL(arquivo) }));
    this.fotos.update((atuais) => [...atuais, ...novas]);
    if (!this.fotoPrincipal()) {
      this.fotoPrincipal.set(novas[0] ?? null);
    }
  }

  protected retirarFoto(foto: FotoFormulario) {
    if ('arquivo' in foto) {
      URL.revokeObjectURL(foto.url);
    }
    const restantes = this.fotos().filter((atual) => atual !== foto);
    this.fotos.set(restantes);
    if (foto === this.fotoPrincipal()) {
      this.fotoPrincipal.set(restantes[0] ?? null);
    }
    this.erroFotos.set('');
  }

  protected salvar() {
    if (!this.pronto() || this.salvando() || this.conflito()) {
      return;
    }

    const campos = this.formulario.getRawValue();
    const dados: DadosReceita = {
      titulo: campos.titulo.trim(),
      descricao: campos.descricao.trim(),
      ingredientes: this.linhasDe(campos.ingredientes),
      etapas: this.linhasDe(campos.etapas),
      horas: campos.horas,
      minutos: campos.minutos,
    };

    if (this.formulario.invalid || !dados.ingredientes.length || !dados.etapas.length) {
      this.falha.set('Informe pelo menos um ingrediente e uma etapa de preparo.');
      return;
    }

    if ((dados.horas !== null && (!Number.isInteger(dados.horas) || dados.horas < 0 || dados.horas > 99))
        || (dados.minutos !== null && (!Number.isInteger(dados.minutos) || dados.minutos < 0 || dados.minutos > 59))) {
      this.falha.set('Informe horas inteiras de 0 a 99 e minutos inteiros de 0 a 59.');
      return;
    }

    this.salvando.set(true);
    this.falha.set('');
    const fotos = this.fotos();
    const principal = this.fotoPrincipal();
    const selecao: FotosParaSalvar = {
      mantidas: fotos.flatMap((foto) => 'original' in foto ? [foto.original] : []),
      arquivos: fotos.flatMap((foto) => 'arquivo' in foto ? [foto.arquivo] : []),
      principal: principal === null ? null : fotos.indexOf(principal),
    };
    const envio = this.id && this.versao !== null
      ? this.api.editar(this.id, this.versao, dados, selecao)
      : this.api.adicionar(dados, selecao);
    envio
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.salvando.set(false)),
      )
      .subscribe({
        next: (receita) => void this.router.navigate(['/receitas', receita.id]),
        error: (erro) => {
          this.conflito.set(erro.status === 409);
          this.falha.set(mensagemDeErro(erro, 'Não foi possível salvar a receita.'));
        },
      });
  }

  private linhasDe(texto: string): string[] {
    return texto.split('\n').map((linha) => linha.trim()).filter(Boolean);
  }
}
