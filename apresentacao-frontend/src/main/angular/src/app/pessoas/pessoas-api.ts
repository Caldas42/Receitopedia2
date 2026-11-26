import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface Pessoa {
  id: string;
  nomeUsuario: string;
  cursor: string;
}

export type ListaPessoas = 'SEGUINDO' | 'TODOS' | 'AMIGOS';

export interface PaginaPessoas {
  pessoas: Pessoa[];
  proximo: string | null;
}

@Injectable({ providedIn: 'root' })
export class PessoasApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/backend/pessoas';

  listar(lista: ListaPessoas, busca: string, cursor = '') {
    return firstValueFrom(this.http.get<PaginaPessoas>(this.url, { params: { lista, busca, cursor } }));
  }

  segue(id: string) {
    return firstValueFrom(this.http.get<{ seguindo: boolean }>(this.seguimento(id)));
  }

  alterar(id: string, seguir: boolean) {
    const rota = this.seguimento(id);
    return firstValueFrom(seguir ? this.http.put<void>(rota, null) : this.http.delete<void>(rota));
  }

  private seguimento(id: string) {
    return this.url + '/' + encodeURIComponent(id) + '/seguimento';
  }
}
