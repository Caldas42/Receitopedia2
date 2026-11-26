import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { catchError, of, switchMap, tap, throwError } from 'rxjs';

export interface UsuarioSessao {
  id: string;
  nomeUsuario: string;
}

@Injectable({ providedIn: 'root' })
export class SessaoApi {
  private readonly http = inject(HttpClient);
  private readonly estado = signal<UsuarioSessao | null>(null);
  readonly usuario = this.estado.asReadonly();

  consultar() {
    return this.http.get<UsuarioSessao>('/backend/sessao').pipe(
      catchError((erro) => erro.status === 401 ? of(null) : throwError(() => erro)),
      tap((usuario) => this.estado.set(usuario)),
    );
  }

  cadastrar(email: string, nomeUsuario: string, senha: string) {
    return this.http.post<void>('/backend/usuarios', { email, nomeUsuario, senha });
  }

  entrar(email: string, senha: string) {
    const dados = new HttpParams().set('email', email).set('senha', senha);
    return this.http.post<void>('/backend/login', dados).pipe(switchMap(() => this.consultar()));
  }

  sair() {
    return this.http.post<void>('/backend/logout', null).pipe(tap(() => this.estado.set(null)));
  }
}
