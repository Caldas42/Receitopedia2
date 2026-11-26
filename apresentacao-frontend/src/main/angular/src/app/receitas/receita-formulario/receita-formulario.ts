import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { SessaoApi } from '../../contas/sessao-api';
import { DadosReceita, mensagemDeErro, ReceitaApi } from '../receita-api';

@Component({
  selector: 'app-receita-formulario',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './receita-formulario.html',
})
export class ReceitaFormulario implements OnInit {
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
        next: ({ dados, versao, dono }) => {
          if (!dono || dono.id !== this.sessao.usuario()?.id) {
            this.falha.set('Somente o dono pode editar esta receita.');
            return;
          }
          this.versao = versao;
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
    const envio = this.id && this.versao !== null
      ? this.api.editar(this.id, this.versao, dados)
      : this.api.adicionar(dados);
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
