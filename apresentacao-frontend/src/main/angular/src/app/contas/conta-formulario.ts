import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, Observable } from 'rxjs';
import { SessaoApi } from './sessao-api';

@Component({
  selector: 'app-conta-formulario',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './conta-formulario.html',
})
export class ContaFormulario {
  private readonly sessao = inject(SessaoApi);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroy = inject(DestroyRef);
  protected readonly cadastro = this.rota.snapshot.data['cadastro'] === true;
  protected readonly enviado = signal(false);
  protected readonly aviso = signal('');
  protected readonly campos = signal<Record<string, string>>({});
  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    email: '',
    nomeUsuario: '',
    senha: '',
  });

  protected enviar() {
    if (this.enviado()) {
      return;
    }
    this.enviado.set(true);
    this.aviso.set('');
    this.campos.set({});
    const { email, nomeUsuario, senha } = this.formulario.getRawValue();
    const operacao: Observable<unknown> = this.cadastro
      ? this.sessao.cadastrar(email, nomeUsuario, senha)
      : this.sessao.entrar(email, senha);

    operacao.pipe(takeUntilDestroyed(this.destroy), finalize(() => this.enviado.set(false))).subscribe({
      next: () => {
        this.formulario.controls.senha.reset();
        void this.router.navigate(this.cadastro ? ['/entrar'] : ['/receitas'],
          this.cadastro ? { queryParams: { cadastro: 'feito' } } : {});
      },
      error: (erro) => {
        this.campos.set(erro.error?.campos ?? {});
        this.aviso.set(
          typeof erro.error?.detail === 'string' ? erro.error.detail : 'Não foi possível concluir. Tente novamente.',
        );
      },
    });
  }

  protected readonly contaCriada = this.rota.snapshot.queryParamMap.get('cadastro') === 'feito';
}
